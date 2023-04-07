package ru.xipho.godvillebotmodern.job

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import ru.xipho.godvillebotmodern.bot.Bot

@Configuration
@EnableScheduling
class CheckHeroState(
    private val bot: Bot
) {
    @Scheduled(fixedRate = 30000)
    fun doCheck() {
        bot.run()
    }

}