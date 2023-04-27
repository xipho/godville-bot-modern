package ru.xipho.godvillebotmodern.bot.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import okhttp3.OkHttpClient

class TelegramWrapper {

    companion object {
        private const val GODVILLE_TELEGRAM_NOTIFIER_TOKEN = "GODVILLE_TELEGRAM_NOTIFIER_TOKEN"
        private const val GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID = "GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID"
    }

    private val logger = mu.KotlinLogging.logger { }

    private val token: String
        get() = System.getenv(GODVILLE_TELEGRAM_NOTIFIER_TOKEN) ?: ""

    private val chatId: Long
        get() = System.getenv(GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID)?.toLong() ?: -1L

    private lateinit var telegramBot: TelegramBot

    init {
        if (token.isNotBlank()) {
            telegramBot = TelegramBot.Builder(token)
                .okHttpClient(OkHttpClient.Builder().build())
                .build()
        } else {
            logger.warn { "Telegram token was not provided. Telegram communication disabled" }
        }
    }

    fun sendMessage(message: String, parseMessage: Boolean = false): Boolean {
        return if (this::telegramBot.isInitialized) {
            val parseMode = if (parseMessage) ParseMode.MarkdownV2 else null
            val response = telegramBot.sendMessage(chatId, message, parseMode)
            response?.isOk ?: false
        } else false
    }

    fun processUpdates(updateProcessor: (String) -> Unit) {
        if (!this::telegramBot.isInitialized) {
            return
        }

        val request = GetUpdates()
        val response = telegramBot.execute(request)
        if (!response.isOk) {
            logger.warn { "Failed to fetch updates" }
            return
        }

        for (update in response.updates()) {
            val text = update.message()?.text()
            val chId = update.message()?.chat()?.id()
            if (chatId == chId && text != null) {
                updateProcessor.invoke(text)
            }
        }

        response.updates().maxOfOrNull { it.updateId() }?.let { lastUpdateId ->
            telegramBot.execute(GetUpdates().apply { offset(lastUpdateId + 1) })
        }

    }

    private fun TelegramBot.sendMessage(chatId: Long, message: String, parseMode: ParseMode? = null): SendResponse? {
        val request = SendMessage(chatId, message)
        parseMode?.let {
            request.parseMode(it)
        }
        return this.execute(request)
    }

}