package ru.xipho.godvillebotmodern.flows

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.async.FlowScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import ru.xipho.godvillebotmodern.bot.flows.HeroState
import ru.xipho.godvillebotmodern.bot.flows.HeroStateProvider
import java.time.Duration

class HeroStateProviderTest {

    private val actionProvider: HeroActionProvider = mock()


    @Test
    fun `WHEN all OK provider returns HeroState`() {

        val eventBus = EventBus()

        val expectedState = HeroState(
            healthPercent = 50,
            money = 1000,
            pranaLevel = 75,
            pranaInAccumulator = 100,
            isPetOk = true,
            moneyForPetHeal = 0
        )

        actionProvider.stub {
            on { getHealthPercent() } doReturn expectedState.healthPercent
            on { getMoney() } doReturn expectedState.money
            on { getCurrentPrana() } doReturn expectedState.pranaLevel
            on { getAccum() } doReturn expectedState.pranaInAccumulator
            on { isPetOk() } doReturn expectedState.isPetOk
            on { getNeededForPetResurrectMoney() } doReturn expectedState.moneyForPetHeal
        }

        val stateProvider = HeroStateProvider(eventBus, actionProvider, Duration.ofMillis(10))
        var state: HeroState? = null

        val job = FlowScope.launch {
            while (eventBus.stateFlow.value == null) {
                delay(5)
            }
            stateProvider.close()
        }

        FlowScope.launch {
            while (job.isActive) {
                state = eventBus.stateFlow.value
            }
        }

        runBlocking {
            job.join()
        }

        state shouldBe expectedState
    }

}