package ru.xipho.godvillebotmodern.bot.telegram

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runInterruptible
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus

class TelegramNotifier(
    private val eventBus: EventBus,
    private val telegramWrapper: TelegramWrapper
) : AutoCloseable {

    private val logger = mu.KotlinLogging.logger { }

    private val job: Job =
        eventBus.botEventFlow.onEach {
            runInterruptible(NotificationScope.coroutineContext) {
                logger.trace("Received event $it")
                val message = "$notifierPrefix ${it.message}"
                logger.trace("Sending message $message")
                telegramWrapper.sendMessage(message)
            }
        }.launchIn(NotificationScope)

    companion object {
        private const val notifierPrefix = "[GodvilleBot]:"
    }

    override fun close() {
        logger.info { "Closing telegram notifier" }
        job.cancel()
        logger.info { "Telegram notifier closed" }
    }
}