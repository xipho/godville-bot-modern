package ru.xipho.godvillebotmodern.bot.settings

import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class BotSettingsManager(
    private val gson: Gson
) {

    private val logger = mu.KotlinLogging.logger {  }

    private lateinit var botSettings: BotSettings
    private val settingsLocation: String
        get() = System.getenv("GODVILLE_BOT_SETTINGS_PATH") ?: ""


    val settings: BotSettings
        get() = botSettings

    init {
        val settingsPath = Paths.get(settingsLocation)
        if (Files.exists(settingsPath)) {
            readExistingSettings(settingsPath)
        } else {
            createDefaultSettings()
        }
    }

    private fun readExistingSettings(settingsPath: Path?) {
        logger.info("Bot settings file found at $settingsLocation. Trying to load")
        Files.newBufferedReader(settingsPath).use {
            botSettings = gson.fromJson(it, BotSettings::class.java)
        }
        logger.info("Bot settings loaded.")
    }

    private fun createDefaultSettings() {
        logger.info("No bot settings file found at $settingsLocation. Creating default one")
        botSettings = BotSettings()
        saveSettings()
        logger.info("Default settings file created at $settingsLocation")
    }



    fun viewSettings(): String = gson.toJson(botSettings)

    fun updateProperty(name: String, value: String) {
        val property = BotSettings::class.memberProperties.find { it.name == name }
        property?.let {
            val mutableProp = it as KMutableProperty<*>
            mutableProp.isAccessible = true
            when (mutableProp.returnType.toString()) {
                "kotlin.Int" -> mutableProp.setter.call(settings, value.toInt())
                "kotlin.Boolean" -> mutableProp.setter.call(settings, value.toBoolean())
            }
            saveSettings()
        }
    }

    private fun saveSettings() {
        val settingsPath = Paths.get(settingsLocation)
        Files.newBufferedWriter(settingsPath, StandardOpenOption.CREATE).use {
            gson.toJson(botSettings, it)
        }
    }
}