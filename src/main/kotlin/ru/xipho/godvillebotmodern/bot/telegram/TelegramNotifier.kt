package ru.xipho.godvillebotmodern.bot.telegram

import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.events.BotEventListener
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.misc.SimpleRateLimiter

class TelegramNotifier(
    private val godvilleBot: GodvilleBot,
    private val telegramWrapper: TelegramWrapper
): BotEventListener, AutoCloseable {

    private val logger = LoggerFactory.getLogger(TelegramNotifier::class.java)
    private val usualLimiter = SimpleRateLimiter(4, 2)
    private val urgentLimiter = SimpleRateLimiter(7, 2)

    companion object {
        private const val notifierPrefix = "[GodvilleBot]:"
    }

    init {
        godvilleBot.subscribeToBotEvent(this)
    }

    override fun close() {
        godvilleBot.unsubscribeFromBotEvent(this)
    }

    override suspend fun invoke(event: BotEvent) {
        logger.trace("Received event $event")
        val message = "$notifierPrefix ${event.message}"
        val limiter = selectLimiter(event)

        limiter.doRateLimited {
            NotificationScope.launch {
                logger.trace("Sending message $message")
                telegramWrapper.sendMessage(message)
            }
        }
    }

    private fun selectLimiter(event: BotEvent) = when (event.urgent) {
        true -> urgentLimiter
        false -> usualLimiter
    }
}