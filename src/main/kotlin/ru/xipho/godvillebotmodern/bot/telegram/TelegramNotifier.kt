package ru.xipho.godvillebotmodern.bot.telegram

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus

class TelegramNotifier(
    private val telegramWrapper: TelegramWrapper
): AutoCloseable {

    private val logger = mu.KotlinLogging.logger {  }

    private val job: Job = NotificationScope.launch {
        EventBus.botEventFlow.onEach {
            runInterruptible(NotificationScope.coroutineContext) {
                logger.trace("Received event $it")
                val message = "$notifierPrefix ${it.message}"
                logger.trace("Sending message $message")
                telegramWrapper.sendMessage(message)
            }
        }
    }

    companion object {
        private const val notifierPrefix = "[GodvilleBot]:"
    }

    override fun close() {
        logger.info { "Closing telegram notifier" }
        job.cancel()
        logger.info { "Telegram notifer closed" }
    }
}