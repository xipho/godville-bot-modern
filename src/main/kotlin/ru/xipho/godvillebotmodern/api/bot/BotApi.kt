package ru.xipho.godvillebotmodern.api.bot

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.repo.ConfigRepo
import java.time.Duration

@Component
class BotApi(
    private val configRepo: ConfigRepo,
) {

    @Value("\${godville.browser.headless}")
    private val headless: Boolean = false

    private val domain: String = "https://godville.net"
    private val mainPage: String = "/superhero"

    private val botSettings: BotSettings
    private val driver: WebDriver

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

        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions()
        options.setHeadless(headless)

        driver = ChromeDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5))
    }

    fun getHealth() {

    }
}