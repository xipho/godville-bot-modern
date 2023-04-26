package ru.xipho.godvillebotmodern.bot

import org.openqa.selenium.PageLoadStrategy
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import java.time.Duration

object WebDriverProvider {

    private lateinit var driver: WebDriver

    fun getDriver(): WebDriver = if (this::driver.isInitialized) {
        driver
    } else {
        driver = when (GlobalConfigProvider.browser) {
            "firefox" -> prepareFirefoxDriver()
            "chrome" -> prepareChromeDriver()
            else -> throw IllegalArgumentException("Unknown browser!")
        }
        driver
    }

    private fun prepareFirefoxDriver(): WebDriver {
        System.setProperty("webdriver.gecko.driver", GlobalConfigProvider.webDriverPath)
        val options = FirefoxOptions()
        options.setHeadless(GlobalConfigProvider.browserHeadless)
        options.profile = FirefoxProfile()
        val driver = FirefoxDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120))
        return driver
    }

    private fun prepareChromeDriver(): WebDriver {
        System.setProperty("webdriver.chrome.driver", GlobalConfigProvider.webDriverPath)
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
        if (GlobalConfigProvider.browserHeadless) {
            options.addArguments("--headless=new")
        }
        val driver = ChromeDriver(options)
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(120))
        return driver
    }
}