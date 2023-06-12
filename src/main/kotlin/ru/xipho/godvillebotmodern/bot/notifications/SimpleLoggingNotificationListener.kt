package ru.xipho.godvillebotmodern.bot.notifications

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.async.NotificationsScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus

class SimpleLoggingNotificationListener(
    private val eventBus: EventBus
): AutoCloseable {

    private val logger = mu.KotlinLogging.logger {  }
    private val job: Job

    init {
        job = NotificationsScope.launch {
            eventBus.botEventFlow.onEach {
                invoke(it)
            }
        }
    }

    override fun close() {
        job.cancel()
    }

    fun invoke(event: BotEvent) {
        logger.info("Event received: $event")
    }
}