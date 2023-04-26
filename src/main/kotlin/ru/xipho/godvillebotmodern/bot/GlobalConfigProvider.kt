package ru.xipho.godvillebotmodern.bot

object GlobalConfigProvider {

        private const val GODVILLE_BROWSER_HEADLESS = "GODVILLE_BROWSER_HEADLESS"
        private const val GODVILLE_BROWSER_USERNAME = "GODVILLE_BROWSER_USERNAME"
        private const val GODVILLE_BROWSER_PASSWORD = "GODVILLE_BROWSER_PASSWORD"
        private const val GODVILLE_BROWSER_NAME = "GODVILLE_BROWSER_NAME"
        private const val GODVILLE_BROWSER_DRIVER_PATH = "GODVILLE_BROWSER_DRIVER_PATH"

    val browserHeadless: Boolean
        get() = System.getenv(GODVILLE_BROWSER_HEADLESS).toBoolean()

    val browserUserName: String
        get() = System.getenv(GODVILLE_BROWSER_USERNAME) ?: ""

    val browserPassword: String
        get() = System.getenv(GODVILLE_BROWSER_PASSWORD) ?: ""

    val browser: String
        get() = System.getenv(GODVILLE_BROWSER_NAME) ?: "chrome"

    val webDriverPath: String
        get() = System.getenv(GODVILLE_BROWSER_DRIVER_PATH) ?: ""
}