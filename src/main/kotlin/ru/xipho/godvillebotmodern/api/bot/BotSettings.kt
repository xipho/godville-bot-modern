package ru.xipho.godvillebotmodern.api.bot

data class BotSettings(
    val headless: Boolean,
    val checkPeriod: Int,
    val checkHealth: Boolean,
    val checkPet: Boolean,
    val healthWarnThreshold: Int,
    val allowPranaExtract: Boolean,
    val maxPranaExtractPerDay: Int,
    val maxPranaExtractPerHour: Int,
)