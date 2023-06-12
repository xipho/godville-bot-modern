package ru.xipho.godvillebotmodern.bot.api.impl

import org.openqa.selenium.WebDriver
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.api.WebDriverProvider
import ru.xipho.godvillebotmodern.bot.settings.GlobalConfigProvider
import ru.xipho.godvillebotmodern.pages.HeroPage
import ru.xipho.godvillebotmodern.pages.LoginPage

class HeroActionProviderImpl : HeroActionProvider {

    private val logger = mu.KotlinLogging.logger { }

    private val driver: WebDriver = WebDriverProvider.getDriver()
    private val loginPage = LoginPage(driver)
    private val heroPage = HeroPage(driver)

    private val moneyRegex = """.*\s(\d+)\s.*""".toRegex()
    private val healthBarPercentRegex = """.*\s(\d+)%.*""".toRegex()

    companion object {
        private const val domain: String = "https://godville.net"
        private const val domain2: String = "https://godville.net/"
        private const val heroPageSuffix: String = "/superhero"
        private const val loginPageSuffix: String = "/login"
    }

    private val isOnHeroPage: Boolean
        get() = driver.currentUrl?.endsWith(heroPageSuffix) ?: false

    private val isOnLoginPage: Boolean
        get() = driver.currentUrl?.let { it == domain || it == domain2 || it.endsWith(loginPageSuffix) } ?: false

    private fun <T> checkOnHeroPageAndDoAction(action: () -> T): T {
        return try {
            if (!isOnHeroPage) {
                logger.trace { "We are not on hero page!" }
                checkLoginPageAndLogin()
            }
            action.invoke()
        } catch (ex: Exception) {
            logger.error(ex) { "Action failed!" }
            throw ex
        }
    }

    private fun checkLoginPageAndLogin() {
        if (!isOnLoginPage) {
            logger.trace { "Not on login page. Going there!" }
            driver.navigate().to(domain2)
        }
        logger.trace { "Trying to login" }
        login(GlobalConfigProvider.browserUserName, GlobalConfigProvider.browserPassword)
    }

    private fun login(userName: String, password: String) {
        try {
            loginPage.inputUsername.sendKeys(userName)
            loginPage.inputPassword.sendKeys(password)
            loginPage.inputCommit.click()
        } catch (ex: Exception) {
            logger.error(ex) {
                ex.message
            }
        }
    }

    override fun getHealth(): Int = checkOnHeroPageAndDoAction {
        val health = heroPage.healthElement.text.split(" / ")[0]
        logger.trace("Hero's health: $health")
        health.toInt()
    }

    override fun getHealthPercent(): Int = checkOnHeroPageAndDoAction {
        val health = heroPage.healthBarElement.getAttribute("title").replace(healthBarPercentRegex, "$1")
        logger.trace("Hero's health: $health%")
        health.toInt()
    }

    override fun getMoney(): Int = checkOnHeroPageAndDoAction {
        val money = heroPage.moneyElement.text.replace(moneyRegex, "$1")
        logger.trace("Hero's money: $money")
        money.toInt()
    }

    override fun getNeededForPetResurrectMoney(): Int = checkOnHeroPageAndDoAction {
        val healMoney = heroPage.petHealMoney.text.replace(moneyRegex, "$1")
        logger.trace("Money to heal pet need: $healMoney")
        if (healMoney.isEmpty()) {
            0
        } else {
            healMoney.toInt()
        }
    }

    override fun getAccum(): Int = checkOnHeroPageAndDoAction {
        logger.trace("Prana in accumulator ${heroPage.pranaAccum.text}")
        heroPage.pranaAccum.text.toInt()
    }

    override fun resurrect() = checkOnHeroPageAndDoAction {
        heroPage.resurrectLink.click()
    }

    override fun getCurrentPrana(): Int = checkOnHeroPageAndDoAction {
        logger.trace("Current Prana level ${heroPage.prana.text}")
        heroPage.prana.text.replace("%", "").toInt()
    }

    override fun makeGood() = checkOnHeroPageAndDoAction {
        heroPage.goodLink.click()
    }

    @Suppress("unused")
    override fun makeBad() = checkOnHeroPageAndDoAction {
        heroPage.badLink.click()
    }

    override fun extractPrana() = checkOnHeroPageAndDoAction {
        val pranaInAccumulator = getAccum()
        if (pranaInAccumulator > 0) {
            heroPage.fillPranaLink.click()
            logger.trace("Extracted prana from accumulator")
            true
        } else {
            logger.warn("Failed to extract prana - value in accum: $pranaInAccumulator")
            false
        }
    }

    override fun isPetOk(): Boolean = checkOnHeroPageAndDoAction {
        !heroPage.petElement.text.contains("❌")
    }

    override fun useFirstPranaFromInventoryIfHave(): Unit = checkOnHeroPageAndDoAction {
        if (heroPage.havePranaInInventory) {
            val first = heroPage.pranaItemsInInventory.first()
            first.click()
            heroPage.pranaItemsInInventory.remove(first)
        }
    }

    override fun close() {
        driver.quit()
    }
}