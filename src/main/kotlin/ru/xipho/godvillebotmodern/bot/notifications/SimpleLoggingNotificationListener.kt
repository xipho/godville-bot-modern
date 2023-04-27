package ru.xipho.godvillebotmodern.bot.notifications

import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.events.BotEventListener

class SimpleLoggingNotificationListener(
    private val bot: GodvilleBot
): BotEventListener, AutoCloseable {

    private val logger = mu.KotlinLogging.logger {  }

    init {
        bot.subscribeToBotEvent(this)
    }

    override fun close() {
        bot.unsubscribeFromBotEvent(this)
    }
    override suspend fun invoke(event: BotEvent) {
        logger.info("Event received: $event")
    }
}