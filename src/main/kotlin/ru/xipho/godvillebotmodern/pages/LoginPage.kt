package ru.xipho.godvillebotmodern.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class LoginPage(
    private val driver: WebDriver
) {

    val inputUsername: WebElement
        get() = driver.findElement(By.cssSelector("input[id='username']"))

    val inputPassword: WebElement
        get() = driver.findElement(By.cssSelector("input[id='password']"))

    val inputCommit: WebElement
        get() = driver.findElement(By.cssSelector("input[name='commit']"))

}