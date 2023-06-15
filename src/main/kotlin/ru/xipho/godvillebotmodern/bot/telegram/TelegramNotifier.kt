package ru.xipho.godvillebotmodern.bot.telegram

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runInterruptible
import ru.xipho.godvillebotmodern.bot.BotEventConstants
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import ru.xipho.godvillebotmodern.bot.misc.ActionRateLimiter
import java.time.Duration

class TelegramNotifier(
    eventBus: EventBus,
    private val telegramWrapper: TelegramWrapper
) : AutoCloseable {

    private val eventsLimiters = mutableMapOf(
        BotEventConstants.BOT_EVENT_PET_BAD_TEXT to ActionRateLimiter(Duration.ofHours(1), 1),
        BotEventConstants.BOT_EVENT_PET_BAD_CAN_HEAL_TEXT to ActionRateLimiter(Duration.ofMinutes(5), 1),
        BotEventConstants.BOT_EVENT_PRANA_ACCUM_EMPTY_TEXT to ActionRateLimiter(Duration.ofHours(1), 1)
    )

    private val logger = mu.KotlinLogging.logger { }

    private val job: Job =
        eventBus.botEventFlow.onEach {
            val limiter = eventsLimiters.computeIfAbsent(it.message) {
                ActionRateLimiter(Duration.ofMinutes(5), 2)
            }
            runInterruptible(NotificationScope.coroutineContext) {
                limiter.doRateLimited {
                    logger.trace("Received event $it")
                    val message = "$notifierPrefix ${it.message}"
                    logger.trace("Sending message $message")
                    telegramWrapper.sendMessage(message)
                }
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