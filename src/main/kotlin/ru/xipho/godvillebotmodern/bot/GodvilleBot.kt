package ru.xipho.godvillebotmodern.bot

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.launch
import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.events.BotEventListener
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsManager
import ru.xipho.godvillebotmodern.pages.HeroPage
import ru.xipho.godvillebotmodern.pages.LoginPage
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

@Component
class GodvilleBot(
    private val botSettingsManager: BotSettingsManager
) {

    private val headless: Boolean
        get() = System.getenv(GODVILLE_BROWSER_HEADLESS)?.toBoolean() ?: false

    private val username: String
        get() = System.getenv(GODVILLE_BROWSER_USERNAME) ?: ""

    private val password: String
        get() = System.getenv(GODVILLE_BROWSER_PASSWORD) ?: ""

    private val browserName: String
        get() = System.getenv(GODVILLE_BROWSER_NAME) ?: "chrome"

    private val webDriverPath: String
        get() = System.getenv(GODVILLE_BROWSER_DRIVER_PATH) ?: ""

    companion object {
        private const val domain: String = "https://godville.net"
        private const val domain2: String = "https://godville.net/"
        private const val heroPage: String = "/superhero"
        private const val GODVILLE_BROWSER_HEADLESS = "GODVILLE_BROWSER_HEADLESS"
        private const val GODVILLE_BROWSER_USERNAME = "GODVILLE_BROWSER_USERNAME"
        private const val GODVILLE_BROWSER_PASSWORD = "GODVILLE_BROWSER_PASSWORD"
        private const val GODVILLE_BROWSER_NAME = "GODVILLE_BROWSER_NAME"
        private const val GODVILLE_BROWSER_DRIVER_PATH = "GODVILLE_BROWSER_DRIVER_PATH"
        private val logger = LoggerFactory.getLogger(GodvilleBot::class.java)
    }

    private val botEventListeners: MutableList<BotEventListener> = mutableListOf()
    private val driver: WebDriver

    private var perDayExtractions: MutableList<LocalDateTime> = mutableListOf()
    private var perHourExtractions: MutableList<LocalDateTime> = mutableListOf()

    @Volatile
    private var runState: AtomicReference<RunState> = AtomicReference(RunState.RUNNING)

    init {
        driver = when(browserName) {
            "chrome" -> prepareChromeDriver()
            "firefox" -> prepareFirefoxDriver()
            else -> throw IllegalArgumentException("Unsupported browser '$browserName'")
        }
    }

    private fun prepareFirefoxDriver(): WebDriver {
        System.setProperty("webdriver.gecko.driver", webDriverPath)
        val options = FirefoxOptions()
        options.setHeadless(headless)
        options.profile = FirefoxProfile()
        val driver = FirefoxDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5))
        return driver
    }

    private fun prepareChromeDriver(): WebDriver {
        System.setProperty("webdriver.chrome.driver", webDriverPath)
        val options = ChromeOptions()
        options.addArguments("--window-size=1280,720")
        options.addArguments("enable-automation")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")
        options.addArguments("--disable-browser-side-navigation")
        options.addArguments("--disable-gpu")
        options.addArguments("--disable-extensions")
        options.addArguments("--dns-prefetch-disable")
        options.addArguments("--disable-gpu")
        options.addArguments("--ignore-ssl-errors")
        options.addArguments("--log-level=3")
        options.addArguments("enable-features=NetworkServiceInProcess")
        options.addArguments("disable-features=NetworkService")
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL)
        if (headless) {
            options.addArguments("--headless=new")
        }
        val driver = ChromeDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120))
        return driver
    }

    fun run() {
        if (runState.get() != RunState.RUNNING) {
            logger.warn("RunState is not RUNNING. Exiting")
            return
        }

        logger.trace("Checking hero state...")

        if (!driver.currentUrl.endsWith(heroPage)) {
            if (!isOnLoginPage) {
                driver.navigate().to(domain)
            }
            runOnLoginPageCycle()
        } else {
            runMainCycle()
        }
    }

    private val isOnLoginPage: Boolean
        get() = driver.currentUrl == domain || driver.currentUrl == domain2 || driver.currentUrl.endsWith("/login")

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
            return
        }

        val page = HeroPage(driver)

        handlePranaLevel(page)
        handlePetCondition(page)
        handleHealthConditions(page)
    }

    private fun handleHealthConditions(page: HeroPage) {
        if (!botSettingsManager.settings.checkHealth) {
            logger.warn("Health check is disabled")
            return
        }
        val healthInPercents = page.getHealthPercent()

        if (healthInPercents < botSettingsManager.settings.healthLowPercentWarningThreshold) {
            if (page.getCurrentPrana() > 25) {
                logger.debug("Healing our hero")
                page.makeGood()
            } else {
                logger.warn("Not enough prana to heal hero")
            }
        }
    }

    private fun handlePetCondition(page: HeroPage) {
        if (!botSettingsManager.settings.checkPet) {
            logger.warn("Pet check is disabled")
            return
        }
        val petOk = page.isPetOk()
        if (!petOk) {
            val money = page.getMoney()
            onBotEvent("\uD83D\uDE31 БЕДА!!! Питомца контузило!!!")
            val neededMoney = page.getNeededForPetRessurrectMoney()
            if (money >= neededMoney) {
                onBotEvent("\uD83E\uDD11 Есть бабло на починку питомца! Действуй!")
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
                onBotEvent("\uD83D\uDE4F Маловато праны, распаковываем из аккумулятора!")
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
            onBotEvent(
                "\uD83E\uDEAB В аккумуляторе закончилась прана! Пополни запасы как можно скорее!",
                true
            )
            logger.warn("No prana in accumulator left!")
            true
        } else {
            false
        }
    }

    private val isPranaExtractionPossible: Boolean
        get() {
            if (!botSettingsManager.settings.allowPranaExtract) {
                onBotEvent("\uD83D\uDE45\u200D♂️ Распаковка праны отключена. Поменяй настройки, если необходимо")
                logger.warn("Prana extraction disabled")
            }
            val currentTime = LocalDateTime.now()
            val maxPerDay = botSettingsManager.settings.maxPranaExtractionsPerDay
            val maxPerHour = botSettingsManager.settings.maxPranaExtractionsPerHour

            perDayExtractions.removeIf { it.isBefore(currentTime.minusDays(1)) }
            perHourExtractions.removeIf { it.isBefore(currentTime.minusHours(1)) }

            val perDayExtractionAvailable = perDayExtractions.size < maxPerDay
            val perHourExtractionAvailable = perHourExtractions.size < maxPerHour

            return if (perDayExtractionAvailable && perHourExtractionAvailable) {
                true
            } else {
                onBotEvent("""\uD83D\uDE10 Не получилось распаковать прану - достигнут один из лимитов: 
                    | Лимит в день: ${!perDayExtractionAvailable}
                    | Лимит в час: ${!perHourExtractionAvailable}
                """.trimMargin(), true)
                logger.warn("Extraction denied due to limits.")
                logger.warn("Per day extraction limit reached: ${!perDayExtractionAvailable}")
                logger.warn("Per hour extraction limit reached: ${!perHourExtractionAvailable}")
                false
            }
        }

    private fun runGracefulShutdownCycle() {
        logger.info("Shutting down")
        driver.quit()
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

    private fun onBotEvent(
        message: String,
        urgent: Boolean = false
    ) {
        val event = BotEvent(message, urgent)
        BotScope.launch {
            botEventListeners.forEach {
                launch { it.invoke(event) }
            }
        }
    }

    @PreDestroy
    fun tearDown() {
        runState.set(RunState.SHUTDOWN)
        runGracefulShutdownCycle()
    }

    enum class RunState {
        RUNNING,
        SHUTDOWN,
    }
}