package ru.xipho.godvillebotmodern.bot

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Semaphore
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.impl.HeroActionProviderImpl
import ru.xipho.godvillebotmodern.bot.async.FlowScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import ru.xipho.godvillebotmodern.bot.flows.HeroState
import ru.xipho.godvillebotmodern.bot.misc.ActionRateLimiter
import ru.xipho.godvillebotmodern.bot.settings.BotSettings
import java.time.Duration

class GodvilleBot(
    private val eventBus: EventBus,
    private val heroActionProvider: HeroActionProvider = HeroActionProviderImpl(),
) : AutoCloseable {
    companion object {
        private val logger = mu.KotlinLogging.logger { }
        private const val MINIMUM_ACCEPTABLE_PRANA_LEVEL = 25
    }

    private lateinit var pranaExtractionLimiter: ActionRateLimiter
    private val semaphore = Semaphore(permits = 1, acquiredPermits = 1)

    init {
        logger.info { "Starting Godville Bot" }
        startHandlingStateEvents()
    }

    private fun startHandlingStateEvents() {
        eventBus.stateFlow.onEach { heroState ->

            if (heroState == null) {
                logger.trace { "Hero state is empty for now. Waiting for actual state..." }
                return@onEach
            }

            logger.trace { "Checking hero state" }

            val settings = eventBus.settingsFlow.value
            try {
                handlePranaLevel(heroState, settings)
                handlePetCondition(heroState, settings)
                handlePossibleHeroDeath(heroState)
                handleHealthConditions(heroState, settings)
            } catch (ex: Exception) {
                logger.error(ex) { "Error occurred while working with page!" }
            }
        }.launchIn(FlowScope)
    }

    private fun handlePossibleHeroDeath(heroState: HeroState) {
        if (heroState.healthPercent == 0) {
            onBotEvent(BotEventConstants.BOT_EVENT_HERO_DEAD_TEXT, urgent = true)
            tryResurrect()
        }
    }

    private fun tryResurrect() {
        try {
            heroActionProvider.resurrect()
        } catch (ex: Exception) {
            onBotEvent(BotEventConstants.BOT_EVENT_FAILED_RESURRECT_HERO_TEXT, urgent = true)
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
                    onBotEvent(BotEventConstants.BOT_EVENT_HEAL_FAILED_TEXT)
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
                onBotEvent(BotEventConstants.BOT_EVENT_PET_BAD_TEXT)
                val money = heroState.money
                val neededMoney = heroState.moneyForPetHeal
                if (neededMoney in 1..money) {
                    onBotEvent(BotEventConstants.BOT_EVENT_PET_BAD_CAN_HEAL_TEXT)
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
                        onBotEvent(BotEventConstants.BOT_EVENT_LOW_PRANA_LEVEL_TEXT)
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
            onBotEvent(BotEventConstants.BOT_EVENT_PRANA_ACCUM_EMPTY_TEXT, true)
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
        eventBus.emitBotEvent(event)
    }

    override fun close() {
        logger.info { "Godville Bot is going to shutdown..." }
        semaphore.release()
        heroActionProvider.close()
    }

    suspend fun wait() {
        semaphore.acquire()
    }
}