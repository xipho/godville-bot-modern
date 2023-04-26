package ru.xipho.godvillebotmodern.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class HeroPage(
    private val driver: WebDriver
) {
    val healthElement: WebElement
        get() = driver.findElement(By.cssSelector("div[id$='health'] div[class='l_val']"))

    val healthBarElement: WebElement
        get() = driver.findElement(By.cssSelector("#hk_health div[class$='p_bar']"))

    val moneyElement: WebElement
        get() = driver.findElement(By.cssSelector("div[id*='gold'] div[class$='val']"))

    val petElement: WebElement
        get() = driver.findElement(By.cssSelector("#hk_pet_name div[class$='val']"))

    val goodLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='enc']"))

    val badLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='pun']"))

    @Suppress("unused")
    val miracleLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='mir']"))

    val fillPranaLink: WebElement
        get() = driver.findElement(By.cssSelector("a[class*='dch']"))

    val petHealMoney: WebElement
        get() = driver.findElement(By.cssSelector("div[class*='pet_rc']"))

    val resurrectLink: WebElement
        get() = driver
            .findElement(By.id("actions"))
            .findElement(By.id("cntrl1"))
            .findElement(By.linkText("Воскресить"))
    val pranaAccum: WebElement
        get() = driver.findElement(By.cssSelector("span[class='acc_val']"))

    val prana: WebElement
        get() = driver.findElement(By.cssSelector("div[class='gp_val']"))


    val pranaItemsInInventory: MutableList<WebElement>
        get() = driver.findElements(By.cssSelector("a[title^='Этот предмет добавляет в полоску прану']"))

    val havePranaInInventory: Boolean
        get() = pranaItemsInInventory.isNotEmpty()
}