package ru.xipho.godvillebotmodern.bot

data class BotSettings(
    val headless: Boolean,
    val checkPeriod: Int,
    val checkHealth: Boolean,
    val checkPet: Boolean,
    val allowPranaExtract: Boolean,
    val maxPranaExtractPerDay: Int,
    val maxPranaExtractPerHour: Int,
)