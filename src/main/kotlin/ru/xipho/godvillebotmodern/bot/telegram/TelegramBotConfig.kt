package ru.xipho.godvillebotmodern.bot.telegram

import com.pengrad.telegrambot.TelegramBot
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelegramBotConfig {

    companion object {
        private const val GODVILLE_TELEGRAM_NOTIFIER_TOKEN = "GODVILLE_TELEGRAM_NOTIFIER_TOKEN"
        private const val GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID = "GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID"
    }
    private val token: String
        get() = System.getenv(GODVILLE_TELEGRAM_NOTIFIER_TOKEN) ?: ""

    @Bean
    fun chatId() = System.getenv(GODVILLE_TELEGRAM_NOTIFIER_CHAT_ID)?.toLong() ?: -1L

    @Bean
    fun telegramBot(): TelegramBot = TelegramBot.Builder(token)
        .okHttpClient(OkHttpClient.Builder().build())
        .build()
}