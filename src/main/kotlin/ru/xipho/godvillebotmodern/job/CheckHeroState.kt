package ru.xipho.godvillebotmodern.job

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import ru.xipho.godvillebotmodern.bot.GodvilleBot
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@Configuration
@EnableScheduling
class CheckHeroState(
    private val bot: GodvilleBot
) {

    private val isChecking = Semaphore(0)

    @Scheduled(fixedRate = 30000)
    fun doCheck() {
        if (isChecking.tryAcquire(1, TimeUnit.SECONDS)) {
            bot.run()
        }
    }

}