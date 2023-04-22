package ru.xipho.godvillebotmodern.job

import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsManager

@Configuration
@EnableScheduling
class CheckHeroStateConfig {

    @Bean
    fun jobDetail(): JobDetail {
        return JobBuilder.newJob(CheckHeroState::class.java)
            .withIdentity("checkHeroState")
            .storeDurably()
            .build()
    }

    @Bean
    fun trigger(
        jobDetail: JobDetail,
        configManager: BotSettingsManager
    ): Trigger {

        val scheduler = SimpleScheduleBuilder
            .simpleSchedule()
            .withIntervalInSeconds(configManager.settings.checkPeriodSeconds)
            .repeatForever()

        return TriggerBuilder.newTrigger()
            .forJob(jobDetail)
            .withIdentity("checkHeroStateTrigger")
            .withSchedule(scheduler)
            .build()
    }
}

@DisallowConcurrentExecution
class CheckHeroState(
    private val bot: GodvilleBot
): Job {
    override fun execute(context: JobExecutionContext?) {
        bot.run()
    }
}