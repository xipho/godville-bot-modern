package ru.xipho.godvillebotmodern.pages

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory

class LoginPage(
    driver: WebDriver
) {

    @FindBy(css = "input[id='username']")
    private lateinit var inputUsername: WebElement

    @FindBy(css = "input[id='password']")
    private lateinit var inputPassword: WebElement

    @FindBy(css = "input[name='commit']")
    private lateinit var inputCommit: WebElement

    init {
        PageFactory.initElements(driver, this)
    }

    fun login(userName: String, password: String) {
        inputUsername.sendKeys(userName)
        inputPassword.sendKeys(password)
        inputCommit.click()
    }
}