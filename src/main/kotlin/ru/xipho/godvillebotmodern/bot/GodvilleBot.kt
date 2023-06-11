package ru.xipho.godvillebotmodern.bot

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.impl.HeroActionProviderImpl
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import ru.xipho.godvillebotmodern.bot.flows.HeroState
import ru.xipho.godvillebotmodern.bot.misc.ActionRateLimiter
import ru.xipho.godvillebotmodern.bot.settings.BotSettings
import java.time.Duration

class GodvilleBot: AutoCloseable {
    companion object {
        private val logger = mu.KotlinLogging.logger { }
        private const val MINIMUM_ACCEPTABLE_PRANA_LEVEL = 25
    }

    private val heroActionProvider: HeroActionProvider = HeroActionProviderImpl()
    private lateinit var pranaExtractionLimiter: ActionRateLimiter
    private val job: Job

    init {
        job = BotScope.launch {
            logger.info { "Starting Godville Bot" }
            while (isActive) {
                logger.trace { "Checking hero state" }
                val settings = EventBus.settingsFlow.value
                val heroState = EventBus.stateFlow.value!!
                try {
                    handlePranaLevel(heroState, settings)
                    handlePetCondition(heroState, settings)
                    handlePossibleHeroDeath(heroState)
                    handleHealthConditions(heroState, settings)
                } catch (ex: Exception) {
                    logger.error(ex) { "Error occurred while working with page!" }
                }
                delay(settings.checkPeriodSeconds * 1000L)
            }
            logger.info { "Godville Bot is going to shutdown..." }
        }
    }

    private fun handlePossibleHeroDeath(heroState: HeroState) {
        if (heroState.healthPercent == 0) {
            onBotEvent("\uD83D\uDE35 Герой всё. Пытаемся воскресить!", urgent = true)
            tryResurrect()
        }
    }

    private fun tryResurrect() {
        try {
            heroActionProvider.resurrect()
        } catch (ex: Exception) {
            onBotEvent("❗️ Воскресить героя не удалось! Требуется вмешательство!", urgent = true)
        }
    }

    private fun handleHealthConditions(heroState: HeroState, settings: BotSettings) {
        if (!settings.checkHealth) {
            logger.warn("Health check is disabled")
            return
        }

        if (heroState.healthPercent < settings.healthLowPercentWarningThreshold) {
            if (heroState.pranaLevel > MINIMUM_ACCEPTABLE_PRANA_LEVEL) {
                logger.debug("Healing our hero")
                try {
                    heroActionProvider.makeGood()
                } catch (ex: Exception) {
                    logger.error(ex) { "Failed to heal hero" }
                    onBotEvent("\uD83D\uDE33 Не удалось вылечить героя! Проверь, что происходит!")
                }
            } else {
                logger.warn("Not enough prana to heal hero")
            }
        }
    }

    private fun handlePetCondition(heroState: HeroState, settings: BotSettings) {
        if (!settings.checkPet) {
            logger.warn("Pet check is disabled")
            return
        }
        try {
            if (!heroState.isPetOk) {
                val money = heroState.money
                onBotEvent("\uD83D\uDE31 БЕДА!!! Питомца контузило!!!")
                val neededMoney = heroState.moneyForPetHeal
                if (neededMoney in 1..money) {
                    onBotEvent("\uD83E\uDD11 Есть бабло на починку питомца! Действуй!")
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to handle pet condition!" }
        }
    }

    private fun handlePranaLevel(heroState: HeroState, settings: BotSettings) {
        try {
            initOrUpdatePranaLimiterIfNeeded(settings)
            val currentPranaLevel = heroState.pranaLevel
            if (currentPranaLevel < MINIMUM_ACCEPTABLE_PRANA_LEVEL) {
                pranaExtractionLimiter.doRateLimited {
                    if (!isPranaAccumulatorEmpty(heroState)) {
                        onBotEvent("\uD83D\uDE4F Маловато праны, распаковываем из аккумулятора!")
                        heroActionProvider.extractPrana()
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to handle prana level" }
        }
    }

    private fun initOrUpdatePranaLimiterIfNeeded(settings: BotSettings) {
        if (
            !this::pranaExtractionLimiter.isInitialized ||
            pranaExtractionLimiter.allowedActions != settings.maxPranaExtractionsPerDay
        ) {
            pranaExtractionLimiter = ActionRateLimiter(
                interval = Duration.ofDays(1),
                allowedActions = settings.maxPranaExtractionsPerDay
            )
        }
    }

    private fun isPranaAccumulatorEmpty(heroState: HeroState): Boolean = try {
        val pranaInAccum = heroState.pranaInAccumulator
        if (pranaInAccum <= 0) {
            onBotEvent(
                "\uD83D\uDED1 В аккумуляторе закончилась прана! Пополни запасы как можно скорее!",
                true
            )
            logger.warn("No prana in accumulator left!")
            true
        } else {
            false
        }
    } catch (ex: Exception) {
        logger.error(ex) { "Failed to check accumulator!" }
        false
    }

    private fun onBotEvent(
        message: String,
        urgent: Boolean = false
    ) {
        val event = BotEvent(message, urgent)
        EventBus.emitBotEvent(event)
    }

    override fun close() {
        logger.info("Shutting down")
        job.cancel()
        heroActionProvider.close()
    }

    suspend fun wait() {
        job.join()
    }
}