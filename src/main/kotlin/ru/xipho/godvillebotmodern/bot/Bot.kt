package ru.xipho.godvillebotmodern.bot

import io.github.bonigarcia.wdm.WebDriverManager
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.events.BotEvent
import ru.xipho.godvillebotmodern.bot.events.BotEventListener
import ru.xipho.godvillebotmodern.pages.HeroPage
import ru.xipho.godvillebotmodern.pages.LoginPage
import ru.xipho.godvillebotmodern.repo.ConfigRepo
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.EmptyCoroutineContext

@Component
class Bot(
    private val configRepo: ConfigRepo,
) {

    @Value("\${godville.browser.headless}")
    private val headless: Boolean = false

    @Value("\${godville.browser.username}")
    private val username: String = ""

    @Value("\${godville.browser.password}")
    private val password: String = ""

    companion object {
        private const val domain: String = "https://godville.net"
        private const val domain2: String = "https://godville.net/"
        private const val heroPage: String = "/superhero"
        private val logger = LoggerFactory.getLogger(Bot::class.java)
    }

    private val botEventListeners: MutableList<BotEventListener> = mutableListOf()

    private val botSettings: BotSettings
    private val driver: WebDriver

    private var perDayExtractions: MutableList<LocalDateTime> = mutableListOf()
    private var perHourExtractions: MutableList<LocalDateTime> = mutableListOf()

    @Volatile
    private var runState: AtomicReference<RunState> = AtomicReference(RunState.INITIAL)

    init {
        val config = configRepo.findById(1).get()
        botSettings = BotSettings(
            headless = headless,
            checkPeriod = config.checkPeriodSeconds,
            checkHealth = config.checkHealth,
            checkPet = config.checkPet,
            allowPranaExtract = config.allowPranaExtract,
            maxPranaExtractPerDay = config.maxPranaExtractionsPerDay,
            maxPranaExtractPerHour = config.maxPranaExtractionsPerHour
        )

        driver = prepareFirefoxDriver()

    }

    private fun prepareFirefoxDriver(): WebDriver {
        WebDriverManager.firefoxdriver().forceDownload().setup()
        val options = FirefoxOptions()
        options.setHeadless(headless)
        options.profile = FirefoxProfile()

        val driver = FirefoxDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5))
        return driver
    }

    private fun prepareChromeDriver(): WebDriver {
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions()
        options.setHeadless(headless)
        val driver = ChromeDriver()
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5))
        return driver
    }

    fun run() {
        when (runState.get()) {
            RunState.INITIAL -> runInitialCycle()
            RunState.ON_LOGIN_PAGE -> runOnLoginPageCycle()
            RunState.RUNNING -> runMainCycle()
            RunState.SHUTDOWN -> runGracefulShutdownCycle()
            RunState.HALTED -> logger.warn("Bot halted after shutdown. Waiting app exit")
            null -> logger.error("Failed to get bot state. Will try on next turn")
        }
    }

    private fun runInitialCycle() {
        logger.info("Starting up...")
        if (driver.currentUrl.endsWith(heroPage)) {
            logger.warn("It's very strange to be here!")
            return
        }
        driver.navigate().to(domain)
        val isOnLoginPage =
            driver.currentUrl == domain || driver.currentUrl == domain2 || driver.currentUrl.endsWith("/login")
        if (isOnLoginPage) {
            runState.set(RunState.ON_LOGIN_PAGE)
            runOnLoginPageCycle()
        } else {
            logger.error(
                """Failed to got to login page. Expected to be on $domain but current page is ${driver.currentUrl}"""
            )
        }
    }

    private fun runOnLoginPageCycle() {
        val page = LoginPage(driver)
        page.login(username, password)
        if (!driver.currentUrl.endsWith(heroPage)) {
            logger.error("Something went wrong! Failed to login! Will try on next cycle")
            return
        }
        runState.set(RunState.RUNNING)
    }

    private fun runMainCycle() {
        if (!driver.currentUrl.endsWith(heroPage)) {
            logger.error("We are not on hero page! Resetting state!")
            runState.set(RunState.INITIAL)
            return
        }

        val page = HeroPage(driver)

        handlePranaLevel(page)
        handlePetCondition(page)
        handleHealthConditions(page)
    }

    private fun handleHealthConditions(page: HeroPage) {
        val healthInPercents = page.getHealthPercent()

        if (healthInPercents < 30) {
            if (page.getCurrentPrana() > 25) {
                logger.debug("Healing our hero")
                page.makeGood()
            } else {
                logger.warn("Not enough prana to heal hero")
            }
        }
    }

    private fun handlePetCondition(page: HeroPage) {
        val petOk = page.isPetOk()
        if (!petOk) {
            val money = page.getMoney()
            onBotEvent("WARNING! The pet is down and needs to heal!")
            val neededMoney = page.getNeededForPetRessurrectMoney()
            if (money >= neededMoney) {
                onBotEvent("You have enough money to heal pet now!")
            }
        }
    }

    private fun handlePranaLevel(page: HeroPage) {
        val currentPranaLevel = page.getCurrentPrana()

        if (currentPranaLevel < 25) {
            if (isPranaAccumulatorEmpty(page)) {
                return
            }
            if (isPranaExtractionPossible) {
                onBotEvent("Low prana level! Filling up from accumulator")
                page.extractPrana()
                val now = LocalDateTime.now()
                perDayExtractions.add(now)
                perHourExtractions.add(now)
            }
        }
    }

    private fun isPranaAccumulatorEmpty(page: HeroPage): Boolean {
        val pranaInAccum = page.getAccum()
        return if (pranaInAccum <= 0) {
            onBotEvent("No prana in accumulator left! Please refill")
            logger.warn("No prana in accumulator left!")
            true
        } else {
            false
        }
    }

    private val isPranaExtractionPossible: Boolean
        get() {
            val currentTime = LocalDateTime.now()
            val maxPerDay = botSettings.maxPranaExtractPerDay
            val maxPerHour = botSettings.maxPranaExtractPerHour

            perDayExtractions.removeIf { it.isBefore(currentTime.minusDays(1)) }
            perHourExtractions.removeIf { it.isBefore(currentTime.minusHours(1)) }

            val perDayExtractionAvailable = perDayExtractions.size < maxPerDay
            val perHourExtractionAvailable = perHourExtractions.size < maxPerHour

            return if (perDayExtractionAvailable && perHourExtractionAvailable) {
                true
            } else {
                logger.warn("Extraction denied due to limits.")
                logger.warn("Per day extraction limit reached: ${!perDayExtractionAvailable}")
                logger.warn("Per hour extraction limit reached: ${!perHourExtractionAvailable}")
                false
            }
        }

    private fun runGracefulShutdownCycle() {
        logger.info("Shutting down")
        driver.quit()
        runState.set(RunState.HALTED)
    }

    fun subscribeToBotEvent(listener: BotEventListener) {
        if (botEventListeners.indexOf(listener) != -1) {
            return
        }
        botEventListeners.add(listener)
        logger.info("New bot event listener subscribed: ${listener.javaClass}")
    }

    fun unsubscribeFromBotEvent(listener: BotEventListener) {
        botEventListeners.remove(listener)
        logger.info("Bot event listener unsubscribed: ${listener.javaClass}")
    }

    private fun onBotEvent(message: String) {
        val event = BotEvent(message)
        BotScope.launch {
            botEventListeners.forEach {
                launch { it.invoke(event) }
            }
        }
    }

    @PreDestroy
    fun tearDown() {
        runState.set(RunState.SHUTDOWN)
        run()
    }

    enum class RunState {
        INITIAL,
        ON_LOGIN_PAGE,
        RUNNING,
        SHUTDOWN,
        HALTED
    }

    object BotScope : CoroutineScope {
        override val coroutineContext = EmptyCoroutineContext +
                ForkJoinPool.commonPool().asCoroutineDispatcher() +
                CoroutineExceptionHandler { context, exception ->
                    logger.error(context.toString(), exception)
                } + CoroutineName("Bot")
    }
}