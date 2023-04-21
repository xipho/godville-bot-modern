package ru.xipho.godvillebotmodern.bot.notifications.sms

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.runInterruptible
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.Bot
import ru.xipho.godvillebotmodern.bot.async.NotificationScope
import ru.xipho.godvillebotmodern.bot.events.BotEvent
import ru.xipho.godvillebotmodern.bot.events.BotEventListener
import java.time.LocalDateTime

@Component
class SmsNotificationListener(
    private val bot: Bot
): BotEventListener {

    @Value("\${notifier.smsc.login}")
    private val smscLogin: String = ""

    @Value("\${notifier.smsc.password}")
    private val smscPassword: String = ""

    @Value("\${notifier.smsc.phone}")
    private val smscPhone: String = ""

    @Value("\${notifier.smsc.per.hour.amount}")
    private val notificationsPerHourAmount: Int = 3

    private lateinit var api: SmscApi

    private val triggeredEvents: MutableList<LocalDateTime> = mutableListOf()

    private val logger = LoggerFactory.getLogger(SmsNotificationListener::class.java)

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