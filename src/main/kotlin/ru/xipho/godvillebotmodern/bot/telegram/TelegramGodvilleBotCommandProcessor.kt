package ru.xipho.godvillebotmodern.bot.telegram

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsProvider
import java.time.Duration

class TelegramGodvilleBotCommandProcessor(
    private val eventBus: EventBus,
    private val telegramWrapper: TelegramWrapper,
    private val botSettingsProvider: BotSettingsProvider,
    private val readMessagesInterval: Duration = Duration.ofSeconds(5)
) : AutoCloseable {

    private val logger = mu.KotlinLogging.logger { }
    private val job: Job

    init {
        job = BotScope.launch {
            while (isActive) {
                telegramWrapper.processUpdates {
                    val commandMessage = it.startsWith("/")
                    if (commandMessage) {
                        processCommandMessage(it)
                    }
                }
                delay(readMessagesInterval.toMillis())
            }
        }
    }

    private fun processCommandMessage(text: String) {
        val fullCommand = text.split(" ")
        when (val cmd = fullCommand[0]) {
            "/config" -> processConfigCommand(fullCommand[1] to fullCommand[2])
            "/start" -> logger.debug("Bot start command received")
            "/viewconf" -> {
                val config = eventBus.settingsFlow.value
                telegramWrapper.sendMessage(
                    """[GodvilleBot] Текущий конфиг: 
                    |```
                    |$config
                    |```
                """.trimMargin(), true
                )
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
            botSettingsProvider.updateProperty(configName, configValue)
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
        logger.info { "Closing telegram command processor" }
        job.cancel()
        logger.info { "Telegram command processor closed" }
    }
}