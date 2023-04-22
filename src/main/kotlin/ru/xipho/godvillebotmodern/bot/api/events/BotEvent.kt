package ru.xipho.godvillebotmodern.bot.api.events

data class BotEvent(
    val message: String,
    val urgent: Boolean = false,
)