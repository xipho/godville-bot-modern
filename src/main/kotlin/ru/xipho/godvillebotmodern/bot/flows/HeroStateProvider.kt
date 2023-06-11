package ru.xipho.godvillebotmodern.bot.flows

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.api.impl.HeroActionProviderImpl
import ru.xipho.godvillebotmodern.bot.async.CustomClosableScope
import java.time.Duration

class HeroStateProvider(
    private val heroActionProvider: HeroActionProvider = HeroActionProviderImpl()
) : AutoCloseable {

    private val scope = CustomClosableScope("HeroStateScope")

    init {
        scope.launch {
            while (this.isActive) {
                getAndEmitHeroState()
                delay(Duration.ofSeconds(10).toMillis())
            }
        }
    }

    private suspend fun getAndEmitHeroState() = runInterruptible(scope.coroutineContext) {
        try {
            EventBus.emitHeroState(
                HeroState(
                    healthPercent = heroActionProvider.getHealthPercent(),
                    money = heroActionProvider.getMoney(),
                    pranaLevel = heroActionProvider.getCurrentPrana(),
                    pranaInAccumulator = heroActionProvider.getAccum(),
                    isPetOk = heroActionProvider.isPetOk(),
                    moneyForPetHeal = getMoneyForPetHeal()
                )
            )
        } catch (ex: Exception) {
            logger.error(ex) {
                "Failed one of get hero state operations. Reason: ${ex.message}"
            }
        }
    }

    private fun getMoneyForPetHeal() = try {
        heroActionProvider.getNeededForPetResurrectMoney()
    } catch (ex: Exception) {
        0
    }

    override fun close() {
        logger.info { "Closing hero state provider" }
        scope.close()
        logger.info { "Hero state provider closed" }
    }

    companion object {
        private val logger = mu.KotlinLogging.logger { }
    }
}