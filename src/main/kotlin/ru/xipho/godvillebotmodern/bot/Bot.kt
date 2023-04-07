package ru.xipho.godvillebotmodern.bot

import io.github.bonigarcia.wdm.WebDriverManager
import jakarta.annotation.PreDestroy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.pages.HeroPage
import ru.xipho.godvillebotmodern.pages.LoginPage
import ru.xipho.godvillebotmodern.repo.ConfigRepo
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

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
        private const val heroPage: String = "/superhero"
        private val logger = LoggerFactory.getLogger(Bot::class.java)
    }

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
            healthWarnThreshold = config.healthLowWarningThreshold,
            allowPranaExtract = config.allowPranaExtract,
            maxPranaExtractPerDay = config.maxPranaExtractionsPerDay,
            maxPranaExtractPerHour = config.maxPranaExtractionsPerHour
        )

        WebDriverManager.firefoxdriver().setup()
        val options = FirefoxOptions()
        options.setHeadless(headless)

        driver = FirefoxDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5))
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
        val isOnLoginPage = driver.currentUrl.equals(domain) || driver.currentUrl.endsWith("/login")
        if (isOnLoginPage) {
            runState.set(RunState.ON_LOGIN_PAGE)
        } else {
            logger.error("Failed to got to login page. Current page is ${driver.currentUrl}")
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

        val currentPranaLevel = page.getCurrentPrana()
        val health = page.getHealth()
        val petOk = page.isPetOk()
        val money = page.getMoney()

        if (currentPranaLevel < 25) {
            if (isPranaExtractionPossible) {
                page.extractPrana()
                val now = LocalDateTime.now()
                perDayExtractions.add(now)
                perHourExtractions.add(now)
            }
        }

        if (health < botSettings.healthWarnThreshold && currentPranaLevel > 25) {
            logger.debug("Healing our hero")
            page.makeGood()
        }
    }

    private val isPranaExtractionPossible: Boolean
        get() {
            val extractionsPerDay =
                perDayExtractions.filter { it.isAfter(LocalDateTime.now().minusDays(1)) }.size
            val extractionsPerHour =
                perHourExtractions.filter { it.isAfter(LocalDateTime.now().minusHours(1)) }.size
            return extractionsPerDay < botSettings.maxPranaExtractPerDay
                    && extractionsPerHour < botSettings.maxPranaExtractPerHour
        }

    private fun runGracefulShutdownCycle() {
        logger.info("Shutting down")
        driver.close()
        runState.set(RunState.HALTED)
    }

    @PreDestroy
    fun tearDown() {
        runState.set(RunState.SHUTDOWN)
    }

    enum class RunState {
        INITIAL,
        ON_LOGIN_PAGE,
        RUNNING,
        SHUTDOWN,
        HALTED
    }
}