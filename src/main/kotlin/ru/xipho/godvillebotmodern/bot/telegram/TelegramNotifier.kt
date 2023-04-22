package ru.xipho.godvillebotmodern.bot.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
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
    private val usualLimiter = SimpleRateLimiter(5, 3)
    private val urgentLimiter = SimpleRateLimiter(10, 5)

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
                val request = SendMessage(chatId, message).parseMode(ParseMode.MarkdownV2)
                bot.execute(request)
            }
        }
    }

    private fun selectLimiter(event: BotEvent) = when (event.urgent) {
        true -> urgentLimiter
        false -> usualLimiter
    }
}