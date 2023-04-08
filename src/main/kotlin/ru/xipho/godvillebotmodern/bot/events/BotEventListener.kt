package ru.xipho.godvillebotmodern.bot.events

fun interface BotEventListener {
    suspend fun invoke(event: BotEvent)
}