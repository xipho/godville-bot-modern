package ru.xipho.godvillebotmodern.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import ru.xipho.godvillebotmodern.dao.ConfigDAO
import ru.xipho.godvillebotmodern.repo.ConfigRepo

@Configuration
class AppConfig(
    private val configRepo: ConfigRepo
) {

    private val logger = LoggerFactory.getLogger(AppConfig::class.java)

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent?) {
        val configExists = configRepo.existsById(1)
        if (!configExists) {
            logger.warn("Main config not found! Generating!")
            val newConfig = ConfigDAO()
            configRepo.save(newConfig)
        }
    }
}