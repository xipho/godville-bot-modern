package ru.xipho.godvillebotmodern.bot.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse


fun TelegramBot.sendMessage(chatId: Long, message: String, parseMode: ParseMode? = null): SendResponse? {
    val request = SendMessage(chatId, message)
    parseMode?.let {
        request.parseMode(it)
    }
    return this.execute(request)
}