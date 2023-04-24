package ru.xipho.godvillebotmodern.pages

import org.openqa.selenium.By
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
    private val healthElement: WebElement
        get() = driver.findElement(By.cssSelector("div[id$='health'] div[class='l_val']"))

    private val healthBarElement: WebElement
        get() = driver.findElement(By.cssSelector("#hk_health div[class$='p_bar']"))

    private val moneyElement: WebElement
        get() = driver.findElement(By.cssSelector("div[id*='gold'] div[class$='val']"))

    private val petElement: WebElement
        get() = driver.findElement(By.cssSelector("#hk_pet_name div[class$='val']"))

    private val goodLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='enc']"))

    private val badLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='pun']"))

    @Suppress("unused")
    private val miracleLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='mir']"))

    private val fillPranaLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='dch']"))

    private val petHealMoney: WebElement
        get() = driver.findElement(By.cssSelector("div[class*='pet_rc']"))

    private val resurrectLink: WebElement
        get() = driver
            .findElement(By.id("actions"))
            .findElement(By.id("cntrl1"))
            .findElement(By.linkText("Воскресить"))

    @FindBy(css = "span[class='acc_val']")
    private lateinit var pranaAccum: WebElement

    @FindBy(css = "div[class='gp_val']")
    private lateinit var prana: WebElement

    private val moneyRegex = """.*\s(\d+)\s.*""".toRegex()
    private val healthBarPercentRegex = """.*\s(\d+)%.*""".toRegex()

    init {
        PageFactory.initElements(driver, this)
    }

    fun getHealth(): Int {
        val health = healthElement.text.split(" / ")[0]
        logger.trace("Hero's health: $health")
        return health.toInt()
    }

    fun getHealthPercent(): Int {
        val health = healthBarElement.getAttribute("title").replace(healthBarPercentRegex, "$1")
        logger.trace("Hero's health: $health%")
        return health.toInt()
    }

    fun getMoney(): Int {
        // .*\s(\d+)\s.*
        val money = moneyElement.text.replace(moneyRegex, "$1")
        logger.trace("Hero's money: $money")
        return money.toInt()
    }

    fun getNeededForPetRessurrectMoney(): Int {
        val healMoney = petHealMoney.text.replace(moneyRegex, "$1")
        logger.trace("Money to heal pet need: $healMoney")
        return  healMoney.toInt()
    }

    fun getAccum(): Int {
        return try {
            logger.trace("Prana in accumulator ${pranaAccum.text}")
            pranaAccum.text.toInt()
        } catch (e: Exception) {
            logger.error("Failed to read prana accum value!", e)
            -1
        }
    }
    fun resurrect(): Boolean = try {
        resurrectLink.click()
        true
    } catch (e: Exception) {
        logger.error("Failed to resurrect!", e)
        false
    }

    fun getCurrentPrana(): Int {
        return try {
            logger.trace("Current Prana level ${prana.text}")
            prana.text.replace("%", "").toInt()
        } catch (e: Exception) {
            logger.error("Unable to read current prana level!", e)
            -1
        }
    }

    fun makeGood(): Boolean = try {
        goodLink.click()
        true
    } catch (e: Exception) {
        logger.error("Failed to make good!", e)
        false
    }

    @Suppress("unused")
    fun makeBad(): Boolean = try {
        badLink.click()
        true
    } catch (e: Exception) {
        logger.error("Failed to make bad!", e)
        false
    }

    fun extractPrana(): Boolean {
        val pranaInAccumulator = getAccum()
        return if (pranaInAccumulator > 0) {
            fillPranaLink.click()
            logger.trace("Extracted prana from accumulator")
            true
        } else {
            logger.warn("Failed to extract prana - value in accum: $pranaInAccumulator")
            false
        }
    }

    fun isPetOk(): Boolean = !petElement.text.contains("❌")

    private val pranaItemsInInventory: MutableList<WebElement>
        get() = driver.findElements(By.cssSelector("a[title^='Этот предмет добавляет в полоску прану']"))

    val havePranaInInventory: Boolean
        get() = pranaItemsInInventory.isNotEmpty()

    fun useFirstPranaFromInventory(): Boolean = if (havePranaInInventory) {
        val first = pranaItemsInInventory.first()
        first.click()
        pranaItemsInInventory.remove(first)
        true
    } else {
        false
    }
}