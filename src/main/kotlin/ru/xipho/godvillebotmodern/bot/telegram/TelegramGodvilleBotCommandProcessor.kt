package ru.xipho.godvillebotmodern.bot.telegram

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsManager

class TelegramGodvilleBotCommandProcessor(
    private val telegramWrapper: TelegramWrapper,
    private val botSettingsManager: BotSettingsManager
): AutoCloseable {

    private val logger = mu.KotlinLogging.logger {  }

    private var isRunning = true
    private lateinit var job: Job

    fun run() {
        job = BotScope.launch {
            while (isRunning) {
                telegramWrapper.processUpdates {
                    val commandMessage = it.startsWith("/")
                    if (commandMessage) {
                        processCommandMessage(it)
                    }
                }
                delay(10000)
            }
        }
    }

    private fun processCommandMessage(text: String) {
        val fullCommand = text.split(" ")
        when (val cmd = fullCommand[0]) {
            "/config" -> processConfigCommand(fullCommand[1] to fullCommand[2])
            "/start" -> logger.debug("Bot start command received")
            "/view-conf" -> {
                val config = botSettingsManager.viewSettings()
                telegramWrapper.sendMessage("""[GodvilleBot] Текущий конфиг: 
                    |```
                    |$config
                    |```
                """.trimMargin(), true)
            }
            else -> {
                logger.warn("Unsupported command $cmd")
                telegramWrapper.sendMessage("❌ Передана неизвестная команда $cmd")
            }
        }
    }

    private fun processConfigCommand(command: Pair<String, String>) {
        val (configName, configValue) = command

        try {
            botSettingsManager.updateProperty(configName, configValue)
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to update property $configName with $configValue" }
            telegramWrapper.sendMessage(
                "\uD83D\uDE35\u200D\uD83D\uDCAB  Не удалось изменить настройку $configValue. Причина: ${ex.message}"
            )
            return
        }

        telegramWrapper.sendMessage("✅ Конфиг '$configName' обновлён. Новое значение: $configValue")
    }

    override fun close() {
        isRunning = false
        runBlocking {
            job.join()
        }
    }
}