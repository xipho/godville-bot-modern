package ru.xipho.godvillebotmodern.bot.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.GetUpdatesResponse
import com.pengrad.telegrambot.response.SendResponse
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsManager

@Component
class TelegramGodvilleBotConfigurator(
    private val bot: TelegramBot,
    private val chatId: Long,
    private val botSettingsManager: BotSettingsManager
) {

    private val logger = LoggerFactory.getLogger(TelegramGodvilleBotConfigurator::class.java)

    private var isRunning = true
    private lateinit var job: Job

    @PostConstruct
    fun run() {
        job = BotScope.launch {
            while (isRunning) {
                val request = GetUpdates()
                val response = bot.execute(request)
                if (response.isOk) {
                    processUpdates(response)
                }
                delay(10000)
            }
        }
    }

    private fun processUpdates(response: GetUpdatesResponse) {
        for (update in response.updates()) {
            val text = update.message()?.text()
            val chId = update.message()?.chat()?.id()
            if (chatId == chId && text != null) {
                val commandMessage = text.startsWith("/")
                if (commandMessage) {
                    processCommandMessage(text)
                }
            }
        }

        response.updates().maxOfOrNull { it.updateId() }?.let { lastUpdateId ->
            bot.execute(GetUpdates().apply { offset(lastUpdateId + 1) })
        }
    }

    private fun processCommandMessage(text: String) {
        val fullCommand = text.split(" ")
        when (val cmd = fullCommand[0]) {
            "/config" -> processConfigCommand(fullCommand[1] to fullCommand[2])
            "/start" -> logger.debug("Bot start command received")
            "/viewconf" -> {
                val config = botSettingsManager.viewSettings()
                bot.sendMessage("""Текущий конфиг: 
                    |```$config```
                """.trimMargin())
            }
            else -> {
                logger.warn("Unsupported command $cmd")
                bot.sendMessage("❌ Передана неизвестная команда $cmd")
            }
        }
    }

    private fun processConfigCommand(command: Pair<String, String>) {
        val configName = command.first
        val configValue = command.second

        when(configName) {
            "checkPet" -> botSettingsManager.updateCheckPet(configValue)
            "checkHealth" -> botSettingsManager.updateCheckHealth(configValue)
            "healthLowWarningThreshold" -> botSettingsManager.updateHealthLowWarningThreshold(configValue)
            "healthLowPercentWarningThreshold" -> botSettingsManager.updateHealthLowPercentWarningThreshold(configValue)
            "allowPranaExtract" -> botSettingsManager.updateAllowPranaExtract(configValue)
            "maxPranaExtractionsPerDay" -> botSettingsManager.updateMaxPranaExtractionsPerDay(configValue)
            "maxPranaExtractionsPerHour" -> botSettingsManager.updateMaxPranaExtractionsPerHour(configValue)
            else -> {
                logger.warn("Unknown config $configName")
                bot.sendMessage("❌ Передан неизвестный конфиг $configName")
                return
            }
        }

        bot.sendMessage("✅ Конфиг '$configName' обновлён. Новое значение: $configValue")
    }

    @PreDestroy
    fun tearDown() {
        isRunning = false
        runBlocking {
            job.join()
        }
    }

    private fun TelegramBot.sendMessage(message: String): SendResponse? {
        val request = SendMessage(chatId, message).parseMode(ParseMode.MarkdownV2)
        return this.execute(request)
    }
}