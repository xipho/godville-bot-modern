package ru.xipho.godvillebotmodern.bot.settings

data class BotSettings(
    val checkPeriodSeconds: Int = 30,
    val checkHealth: Boolean = true,
    val checkPet: Boolean = true,
    val healthLowWarningThreshold: Int = 150,
    val healthLowPercentWarningThreshold: Int = 30,
    val allowPranaExtract: Boolean = true,
    val maxPranaExtractionsPerDay: Int = 10,
    val maxPranaExtractionsPerHour: Int = 2,
)