package ru.xipho.godvillebotmodern.bot.notifications

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.events.BotEvent
import ru.xipho.godvillebotmodern.bot.events.BotEventListener

@Component
class SimpleLoggingNotificationListener(
    private val bot: GodvilleBot
): BotEventListener  {

    private val logger = LoggerFactory.getLogger(SimpleLoggingNotificationListener::class.java)

    @PostConstruct
    fun init() {
        bot.subscribeToBotEvent(this)
    }

    @PreDestroy
    fun tearDown() {
        bot.unsubscribeFromBotEvent(this)
    }
    override suspend fun invoke(event: BotEvent) {
        logger.info("Event received: $event")
    }
}