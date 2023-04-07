package ru.xipho.godvillebotmodern.api.bot.pages

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory
import org.slf4j.LoggerFactory

class HeroPage(
    private val driver: WebDriver
) {

    companion object {
        private val logger = LoggerFactory.getLogger(HeroPage::class.java)
    }

    @FindBy(css = "div[id$='health'] div[class='l_val']")
    private lateinit var healthElement: WebElement

    @FindBy(css = "div[id*='gold'] div[class$='val']")
    private lateinit var moneyElement: WebElement

    @FindBy(css = "#hk_pet_name div[class$='val']")
    private lateinit var petElement: WebElement

    @FindBy(css = "a[class*='enc']")
    private lateinit var goodLink: WebElement

    @FindBy(css = "a[class*='pun']")
    private lateinit var badLink: WebElement

    @FindBy(css = "a[class*='mir']")
    private lateinit var miracleLink: WebElement

    @FindBy(css = "a[class*='dch']")
    private lateinit var fillPranaLink: WebElement

    @FindBy(css = "span[class='acc_val']")
    private lateinit var pranaAccum: WebElement

    @FindBy(css = "div[class='gp_val']")
    private lateinit var prana: WebElement

    @FindBy(css = "a[title^='Этот предмет добавляет в полоску прану']")
    private lateinit var pranaInventory: WebElement

    private val moneyRegex = """.*\s(\d+)\s.*""".toRegex()

    init {
        PageFactory.initElements(driver, this)
    }

    fun getHealth(): Int {
        val health = healthElement.text.split(" / ")[0]
        logger.trace("Hero's healths: $health")
        return health.toInt()
    }

    fun getMoney(): Int {
        // .*\s(\d+)\s.*
        val money = moneyElement.text.replace(moneyRegex, "$1")
        logger.trace("Hero's money: $money")
        return money.toInt()
    }

    fun getAccum(): Int {
        return try {
            pranaAccum.text.toInt()
        } catch (e: Exception) {
            logger.error("Failed to read prana accum value!", e)
            -1
        }
    }

    fun getCurrentPrana(): Int {
        return try {
            prana.text.replace("%", "").toInt()
        } catch (e: Exception) {
            logger.error("Unable to read current prana level!", e)
            -1
        }
    }

    fun makeGood(): Boolean {
        val currentPrana = getCurrentPrana()
        return if (currentPrana > 25) {
            goodLink.click()
            true
        } else {
            logger.warn("Not enough prana level to make good! $currentPrana")
            false
        }
    }
}