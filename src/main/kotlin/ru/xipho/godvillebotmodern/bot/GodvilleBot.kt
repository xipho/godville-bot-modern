package ru.xipho.godvillebotmodern.bot

import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.api.events.BotEventListener
import ru.xipho.godvillebotmodern.bot.api.impl.HeroActionProviderImpl
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.settings.BotSettingsManager
import java.time.LocalDateTime

@Component
class GodvilleBot(
    private val botSettingsManager: BotSettingsManager
) : AutoCloseable {
    companion object {
        private val logger = mu.KotlinLogging.logger { }
        private const val MINIMUM_ACCEPTABLE_PRANA_LEVEL = 25
        private const val MAXIMUM_ACCEPTABLE_PRANA_LEVEL = 75
    }

    private val botEventListeners: MutableList<BotEventListener> = mutableListOf()

    private var perDayExtractions: MutableList<LocalDateTime> = mutableListOf()
    private var perHourExtractions: MutableList<LocalDateTime> = mutableListOf()

    private val heroActionProvider: HeroActionProvider = HeroActionProviderImpl()

    fun run() {
        try {
            handlePranaLevel()
            handlePetCondition()
            handlePossibleHeroDeath()
            handleHealthConditions()
            handlePranaFromInventory()
        } catch (ex: Exception) {
            logger.error(ex) { "Error occurred while working with page!" }
        }
    }

    private fun handlePossibleHeroDeath() {
        val heroHealth = try {
            heroActionProvider.getHealth()
        } catch (ex: Exception) {
            logger.error(ex) { "failed to get hero health" }
            return
        }

        if (heroHealth == 0) {
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

    private fun handlePranaFromInventory() {
        try {
            if (heroActionProvider.getCurrentPrana() < MAXIMUM_ACCEPTABLE_PRANA_LEVEL) {
                logger.trace { "Using prana from inventory if have it" }
                heroActionProvider.useFirstPranaFromInventoryIfHave()
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to fill prana from inventory" }
        }
    }

    private fun handleHealthConditions() {
        if (!botSettingsManager.settings.checkHealth) {
            logger.warn("Health check is disabled")
            return
        }
        try {
            val healthInPercents = heroActionProvider.getHealthPercent()

            if (healthInPercents < botSettingsManager.settings.healthLowPercentWarningThreshold) {
                if (heroActionProvider.getCurrentPrana() > MINIMUM_ACCEPTABLE_PRANA_LEVEL) {
                    logger.debug("Healing our hero")
                    heroActionProvider.makeGood()
                } else {
                    logger.warn("Not enough prana to heal hero")
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to check health conditions!" }
        }
    }

    private fun handlePetCondition() {
        if (!botSettingsManager.settings.checkPet) {
            logger.warn("Pet check is disabled")
            return
        }
        try {
            val petOk = heroActionProvider.isPetOk()
            if (!petOk) {
                val money = heroActionProvider.getMoney()
                onBotEvent("\uD83D\uDE31 БЕДА!!! Питомца контузило!!!")
                val neededMoney = heroActionProvider.getNeededForPetResurrectMoney()
                if (money >= neededMoney) {
                    onBotEvent("\uD83E\uDD11 Есть бабло на починку питомца! Действуй!")
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to handle pet condition!" }
        }
    }

    private fun handlePranaLevel() {
        try {
            val currentPranaLevel = heroActionProvider.getCurrentPrana()

            if (currentPranaLevel < MINIMUM_ACCEPTABLE_PRANA_LEVEL) {
                if (isPranaAccumulatorEmpty) {
                    return
                }
                if (isPranaExtractionPossible) {
                    onBotEvent("\uD83D\uDE4F Маловато праны, распаковываем из аккумулятора!")
                    heroActionProvider.extractPrana()
                    val now = LocalDateTime.now()
                    perDayExtractions.add(now)
                    perHourExtractions.add(now)
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to handle prana level" }
        }
    }

    private val isPranaAccumulatorEmpty: Boolean
        get() = try {
            val pranaInAccum = heroActionProvider.getAccum()
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

    private val isPranaExtractionPossible: Boolean
        get() {
            if (!botSettingsManager.settings.allowPranaExtract) {
                onBotEvent("⛔️ Распаковка праны отключена. Поменяй настройки, если необходимо")
                logger.warn("Prana extraction disabled")
            }
            val currentTime = LocalDateTime.now()
            val maxPerDay = botSettingsManager.settings.maxPranaExtractionsPerDay
            val maxPerHour = botSettingsManager.settings.maxPranaExtractionsPerHour

            perDayExtractions.removeIf { it.isBefore(currentTime.minusDays(1)) }
            perHourExtractions.removeIf { it.isBefore(currentTime.minusHours(1)) }

            val perDayExtractionAvailable = perDayExtractions.size < maxPerDay
            val perHourExtractionAvailable = perHourExtractions.size < maxPerHour

            return if (perDayExtractionAvailable && perHourExtractionAvailable) {
                true
            } else {
                onBotEvent(
                    """🙅‍♂️ Не получилось распаковать прану - достигнут один из лимитов: 
                    | Лимит в день: ${!perDayExtractionAvailable}
                    | Лимит в час: ${!perHourExtractionAvailable}
                """.trimMargin(), true
                )
                logger.warn { "Extraction denied due to limits." }
                logger.warn { "Per day extraction limit reached: ${!perDayExtractionAvailable}" }
                logger.warn { "Per hour extraction limit reached: ${!perHourExtractionAvailable}" }
                false
            }
        }

    fun subscribeToBotEvent(listener: BotEventListener) {
        if (botEventListeners.indexOf(listener) != -1) {
            logger.warn { "${listener.javaClass} already subscribed" }
            return
        }
        botEventListeners.add(listener)
        logger.info { "New bot event listener subscribed: ${listener.javaClass}" }
    }

    fun unsubscribeFromBotEvent(listener: BotEventListener) {
        botEventListeners.remove(listener)
        logger.info { "Bot event listener unsubscribed: ${listener.javaClass}" }
    }

    private fun onBotEvent(
        message: String,
        urgent: Boolean = false
    ) {
        val event = BotEvent(message, urgent)
        BotScope.launch {
            botEventListeners.forEach {
                launch { it.invoke(event) }
            }
        }
    }

    override fun close() {
        logger.info("Shutting down")
        heroActionProvider.close()
    }
}