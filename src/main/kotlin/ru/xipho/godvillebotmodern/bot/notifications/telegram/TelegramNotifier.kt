package ru.xipho.godvillebotmodern.bot.notifications.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.events.BotEvent
import ru.xipho.godvillebotmodern.bot.events.BotEventListener

@Component
class TelegramNotifier(
    private val godvilleBot: GodvilleBot
): BotEventListener {

    companion object {
        private const val GODVILLE_TELEGRAM_NOTIFIER_TOKEN = "GODVILLE_TELEGRAM_NOTIFIER_TOKEN"
        private const val GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID = "GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID"
    }
    private val token: String
        get() = System.getenv(GODVILLE_TELEGRAM_NOTIFIER_TOKEN) ?: ""

    private val chatId: Long
        get() = System.getenv(GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID)?.toLong() ?: -1L

    private val bot = TelegramBot.Builder(token)
        .okHttpClient(OkHttpClient.Builder().build())
        .build()

    private val logger = LoggerFactory.getLogger(TelegramNotifier::class.java)

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
        val request = SendMessage(chatId, event.message)
        bot.execute(request)
    }
}