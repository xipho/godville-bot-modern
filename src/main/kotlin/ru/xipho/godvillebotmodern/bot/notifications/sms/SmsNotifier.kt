package ru.xipho.godvillebotmodern.bot.notifications.sms

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.runInterruptible
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.Bot
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.events.BotEvent
import ru.xipho.godvillebotmodern.bot.events.BotEventListener
import java.time.LocalDateTime

@Component
class SmsNotifier(
    private val bot: Bot
): BotEventListener {

    companion object {
        private const val GODVILLE_NOTIFIER_SMSC_LOGIN = "GODVILLE_NOTIFIER_SMSC_LOGIN"
        private const val GODVILLE_NOTIFIER_SMSC_PASSWORD = "GODVILLE_NOTIFIER_SMSC_PASSWORD"
        private const val GODVILLE_NOTIFIER_SMSC_PHONE = "GODVILLE_NOTIFIER_SMSC_PHONE"
        private const val GODVILLE_NOTIFIER_SMSC_PER_HOUR_AMOUNT = "GODVILLE_NOTIFIER_SMSC_PER_HOUR_AMOUNT"
    }

    private val smscLogin: String
        get() = System.getenv(GODVILLE_NOTIFIER_SMSC_LOGIN) ?: ""

    private val smscPassword: String
        get() = System.getenv(GODVILLE_NOTIFIER_SMSC_PASSWORD) ?: ""

    private val smscPhone: String
        get() = System.getenv(GODVILLE_NOTIFIER_SMSC_PHONE) ?: ""

    private val notificationsPerHourAmount: Int
        get() = System.getenv(GODVILLE_NOTIFIER_SMSC_PER_HOUR_AMOUNT)?.toInt() ?: 3

    private lateinit var api: SmscApi

    private val triggeredEvents: MutableList<LocalDateTime> = mutableListOf()
    private val logger = LoggerFactory.getLogger(SmsNotifier::class.java)

    @PostConstruct
    fun init() {
        api = SmscApi(smscLogin, smscPassword)
        bot.subscribeToBotEvent(this)
    }

    @PreDestroy
    fun tearDown() {
        bot.unsubscribeFromBotEvent(this)
    }

    override suspend fun invoke(event: BotEvent) {
        runInterruptible(NotificationScope.coroutineContext) {
            logger.trace("Received event $event. Trying to send SMS")
            sendSmsIfAvailable(event)
        }
    }

    private fun sendSmsIfAvailable(event: BotEvent) = if (isSendAvailable) {
        api.sendSms(smscPhone, event.message)
    } else {
        logger.warn("Can't send sms notification. Reason: per hour limit reached")
        ""
    }

    private val isSendAvailable: Boolean
        get() {
            val allowedTime = LocalDateTime.now().minusHours(1)
            triggeredEvents.removeIf { it.isBefore(allowedTime) }
            return triggeredEvents.size <= notificationsPerHourAmount
        }
}