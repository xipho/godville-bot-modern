package ru.xipho.godvillebotmodern.bot.settings

import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
class BotSettingsManager {

    private val logger = LoggerFactory.getLogger(BotSettingsManager::class.java)

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private lateinit var botSettings: BotSettings
    private val settingsLocation: String
        get() = System.getenv("GODVILLE_BOT_SETTINGS_PATH") ?: ""


    val settings: BotSettings
        get() = botSettings

    init {
        val settingsPath = Paths.get(settingsLocation)
        if (Files.exists(settingsPath)) {
            logger.info("Bot settings file found at $settingsLocation. Trying to load")
            Files.newBufferedReader(settingsPath).use {
                botSettings = gson.fromJson(it, BotSettings::class.java)
            }
            logger.info("Bot settings loaded.")
        } else {
            logger.info("No bot settings file found at $settingsLocation. Creating default one")
            botSettings = BotSettings()
            Files.newBufferedWriter(settingsPath, StandardOpenOption.CREATE).use {
                gson.toJson(botSettings, it)
            }
            logger.info("Default settings file created at $settingsLocation")
        }
    }

    fun updateCheckHealth(newValue: String) {
        updateSettings(
            botSettings.copy(
                checkHealth = newValue.toBoolean()
            )
        )
    }

    fun updateCheckPet(newValue: String) {
        updateSettings(
            botSettings.copy(
                checkPet = newValue.toBoolean()
            )
        )
    }

    fun updateHealthLowWarningThreshold(newValue: String) {
        updateSettings(
            botSettings.copy(
                healthLowWarningThreshold = newValue.toInt()
            )
        )
    }

    fun updateHealthLowPercentWarningThreshold(newValue: String) {
        updateSettings(
            botSettings.copy(
                healthLowWarningThreshold = newValue.toInt()
            )
        )
    }

    fun updateAllowPranaExtract(newValue: String) {
        updateSettings(
            botSettings.copy(
                allowPranaExtract = newValue.toBoolean()
            )
        )
    }
    fun updateMaxPranaExtractionsPerDay(newValue: String) {
        updateSettings(
            botSettings.copy(
                maxPranaExtractionsPerDay = newValue.toInt()
            )
        )
    }

    fun updateMaxPranaExtractionsPerHour(newValue: String) {
        updateSettings(
            botSettings.copy(
                maxPranaExtractionsPerHour = newValue.toInt()
            )
        )
    }

    private fun updateSettings(newSettings: BotSettings) {
        botSettings = newSettings
        val settingsPath = Paths.get(settingsLocation)
        Files.newBufferedWriter(settingsPath, StandardOpenOption.CREATE).use {
            gson.toJson(botSettings, it)
        }
    }
}