package ru.xipho.godvillebotmodern.bot.settings

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.xipho.godvillebotmodern.bot.async.CustomClosableScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Duration
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.toKotlinDuration

class BotSettingsProvider(
    private val gson: Gson
) : AutoCloseable {

    private val settingsScope = CustomClosableScope("SettingsScope")
    private val logger = mu.KotlinLogging.logger { }

    private val settingsLocation: String
        get() = System.getenv("GODVILLE_BOT_SETTINGS_PATH") ?: ""

    init {
        settingsScope.launch {
            var settings: BotSettings? = null
            while (this.isActive) {
                val settingsPath = Paths.get(settingsLocation)
                val newSettings = actualizeSettings(settingsPath)
                if (newSettings != settings) {
                    EventBus.emitSettingsChange(newSettings)
                    settings = newSettings
                }
                delay(Duration.ofSeconds(10).toKotlinDuration())
            }
        }
    }

    private fun actualizeSettings(settingsPath: Path) = if (Files.exists(settingsPath)) {
        readExistingSettings(settingsPath)
    } else {
        createDefaultSettings()
    }

    private fun readExistingSettings(settingsPath: Path): BotSettings {
        logger.trace("Bot settings file found at $settingsLocation. Trying to load")
        val botSettings = Files.newBufferedReader(settingsPath).use {
            gson.fromJson(it, BotSettings::class.java)
        }
        logger.trace { "Bot settings loaded." }
        return botSettings
    }

    private fun createDefaultSettings(): BotSettings {
        logger.trace("No bot settings file found at $settingsLocation. Creating default one")
        val defaultSettings = BotSettings()
        saveSettings(defaultSettings)
        logger.trace("Default settings file created at $settingsLocation")
        return defaultSettings
    }

    private fun saveSettings(settings: BotSettings) {
        val settingsPath = Paths.get(settingsLocation)
        Files.newBufferedWriter(settingsPath, StandardOpenOption.CREATE).use {
            gson.toJson(settings, it)
        }
    }

    fun updateProperty(name: String, value: String) {
        val settings = EventBus.settingsFlow.value
        val property = BotSettings::class.memberProperties.find { it.name == name }
        property?.let {
            val mutableProp = it as KMutableProperty<*>
            mutableProp.isAccessible = true
            when (mutableProp.returnType.toString()) {
                "kotlin.Int" -> mutableProp.setter.call(settings, value.toInt())
                "kotlin.Boolean" -> mutableProp.setter.call(settings, value.toBoolean())
            }
            EventBus.emitSettingsChange(settings)
            saveSettings(settings)
        }
    }

    override fun close() {
        logger.info { "Closing settings provider" }
        settingsScope.close()
        logger.info { "Settings provider closed" }
    }
}