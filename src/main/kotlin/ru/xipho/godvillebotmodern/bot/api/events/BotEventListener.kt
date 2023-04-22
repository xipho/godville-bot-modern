package ru.xipho.godvillebotmodern.bot.api.events

fun interface BotEventListener {
    suspend fun invoke(event: BotEvent)
}