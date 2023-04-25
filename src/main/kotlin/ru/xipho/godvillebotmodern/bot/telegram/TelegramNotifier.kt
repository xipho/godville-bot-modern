package ru.xipho.godvillebotmodern.bot.telegram

import com.pengrad.telegrambot.TelegramBot
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.events.BotEventListener
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.misc.SimpleRateLimiter

@Component
class TelegramNotifier(
    private val godvilleBot: GodvilleBot,
    private val bot: TelegramBot,
    private val chatId: Long
): BotEventListener {

    private val logger = LoggerFactory.getLogger(TelegramNotifier::class.java)
    private val usualLimiter = SimpleRateLimiter(4, 2)
    private val urgentLimiter = SimpleRateLimiter(7, 2)

    companion object {
        private const val notifierPrefix = "[GodvilleBot]:"
    }

    @PostConstruct
    fun init() {
        godvilleBot.subscribeToBotEvent(this)
    }

    @PreDestroy
    fun tearDown() {
        godvilleBot.unsubscribeFromBotEvent(this)
    }

    override suspend fun invoke(event: BotEvent) {
        logger.trace("Received event $event")
        val message = "$notifierPrefix ${event.message}"
        val limiter = selectLimiter(event)

        limiter.doRateLimited {
            NotificationScope.launch {
                logger.trace("Sending message $message")
                bot.sendMessage(chatId, message)
            }
        }
    }

    private fun selectLimiter(event: BotEvent) = when (event.urgent) {
        true -> urgentLimiter
        false -> usualLimiter
    }
}