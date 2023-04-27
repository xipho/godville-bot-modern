package ru.xipho.godvillebotmodern

import com.google.gson.GsonBuilder
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.misc.AnnotatedWithExcludeFieldExclusionStrategy
import ru.xipho.godvillebotmodern.bot.notifications.SimpleLoggingNotificationListener
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsManager
import ru.xipho.godvillebotmodern.bot.telegram.TelegramGodvilleBotConfigurator
import ru.xipho.godvillebotmodern.bot.telegram.TelegramNotifier
import ru.xipho.godvillebotmodern.bot.telegram.TelegramWrapper

private lateinit var telegramNotifier: TelegramNotifier
private lateinit var simpleNotifier: SimpleLoggingNotificationListener
private lateinit var botConfigurator: TelegramGodvilleBotConfigurator
private lateinit var settingsManager: BotSettingsManager

suspend fun main() {
	val gson = GsonBuilder()
		.setPrettyPrinting()
		.addSerializationExclusionStrategy(AnnotatedWithExcludeFieldExclusionStrategy)
		.create()
	settingsManager = BotSettingsManager(gson)
	val bot = GodvilleBot(settingsManager)
	val telegramWrapper = TelegramWrapper()
	telegramNotifier = TelegramNotifier(bot, telegramWrapper)
	botConfigurator = TelegramGodvilleBotConfigurator(telegramWrapper, settingsManager)
	botConfigurator.run()
	simpleNotifier = SimpleLoggingNotificationListener(bot)

	Runtime.getRuntime().addShutdownHook(object:Thread() {
		override fun run() {
			bot.close()
		}
	})
	bot.run().join()
}