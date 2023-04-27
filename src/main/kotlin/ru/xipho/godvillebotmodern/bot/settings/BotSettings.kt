package ru.xipho.godvillebotmodern.bot.settings

data class BotSettings(
    var checkPeriodSeconds: Int = 30,
    var checkHealth: Boolean = true,
    var checkPet: Boolean = true,
    var healthLowWarningThreshold: Int = 150,
    var healthLowPercentWarningThreshold: Int = 30,
    var allowPranaExtract: Boolean = true,
    var maxPranaExtractionsPerDay: Int = 10,
    var maxPranaExtractionsPerHour: Int = 2,
)