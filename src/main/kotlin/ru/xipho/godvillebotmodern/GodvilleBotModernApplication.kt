package ru.xipho.godvillebotmodern

import com.google.gson.GsonBuilder
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.flows.HeroStateProvider
import ru.xipho.godvillebotmodern.bot.misc.AnnotatedWithExcludeFieldExclusionStrategy
import ru.xipho.godvillebotmodern.bot.notifications.SimpleLoggingNotificationListener
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsProvider
import ru.xipho.godvillebotmodern.bot.telegram.TelegramGodvilleBotCommandProcessor
import ru.xipho.godvillebotmodern.bot.telegram.TelegramNotifier
import ru.xipho.godvillebotmodern.bot.telegram.TelegramWrapper

private lateinit var telegramNotifier: TelegramNotifier
private lateinit var simpleNotifier: SimpleLoggingNotificationListener
private lateinit var botConfigurator: TelegramGodvilleBotCommandProcessor
private lateinit var settingsProvider: BotSettingsProvider
private lateinit var stateProvider: HeroStateProvider

suspend fun main() {
	val gson = GsonBuilder()
		.setPrettyPrinting()
		.addSerializationExclusionStrategy(AnnotatedWithExcludeFieldExclusionStrategy)
		.create()
	settingsProvider = BotSettingsProvider(gson)
	stateProvider = HeroStateProvider()
	val bot = GodvilleBot()
	simpleNotifier = SimpleLoggingNotificationListener()
	val telegramWrapper = TelegramWrapper()
	telegramNotifier = TelegramNotifier(telegramWrapper)
	botConfigurator = TelegramGodvilleBotCommandProcessor(telegramWrapper, settingsProvider)

	Runtime.getRuntime().addShutdownHook(object:Thread() {
		override fun run() {
			bot.close()
			stateProvider.close()
			settingsProvider.close()
			telegramNotifier.close()
			botConfigurator.close()
		}
	})

	bot.wait()
}