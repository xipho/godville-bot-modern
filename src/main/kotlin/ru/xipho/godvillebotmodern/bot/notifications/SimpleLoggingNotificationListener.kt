package ru.xipho.godvillebotmodern.bot.notifications

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogger
import mu.KotlinLogging
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus

class SimpleLoggingNotificationListener(
    eventBus: EventBus
) {

    private val logger: KLogger = KotlinLogging.logger { }

    init {
        eventBus.botEventFlow.onEach {
            logger.info("Event received: $it")
        }.launchIn(NotificationScope)
    }
}