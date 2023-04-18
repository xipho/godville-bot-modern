package ru.xipho.godvillebotmodern.bot.notifications.sms

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.Bot
import ru.xipho.godvillebotmodern.bot.events.BotEvent
import ru.xipho.godvillebotmodern.bot.events.BotEventListener

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

    private lateinit var api: SmscApi

    @PostConstruct
    fun init() {
        api = SmscApi(smscLogin, smscPassword)
        bot.subscribeToBotEvent(this)
    }
    override suspend fun invoke(event: BotEvent) {
        api.sendSms(smscPhone, event.message)
    }
}