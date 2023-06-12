package ru.xipho.godvillebotmodern.bot

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.*
import ru.xipho.godvillebotmodern.bot.api.HeroActionProvider
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.async.BotScope
import ru.xipho.godvillebotmodern.bot.async.NotificationsScope
import ru.xipho.godvillebotmodern.bot.flows.EventBus
import ru.xipho.godvillebotmodern.bot.flows.HeroState

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GodvilleBotTest {

    private val actionProvider: HeroActionProvider = mock {
        on { extractPrana() } doReturn true
    }

    private val waitDelay = 10L

    @Test
    fun `WHEN low prana THEN extracting prana from accum`() {
        runBlocking {
            val eventBus = EventBus()
            val testState = HeroState(
                healthPercent = 50,
                money = 1000,
                pranaLevel = 18,
                pranaInAccumulator = 100,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            verify(actionProvider).extractPrana()
        }
    }

    @Test
    fun `WHEN low prana AND accum empty THEN notification event of empty accum`() {
        runBlocking {

            val eventBus = EventBus()

            val testState = HeroState(
                healthPercent = 50,
                money = 1000,
                pranaLevel = 18,
                pranaInAccumulator = 0,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            val events = mutableListOf<BotEvent>()
            BotScope.launch {
                eventBus.botEventFlow.toCollection(events)
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            events.size shouldBe 1
            events.any { it.message == Constants.BOT_EVENT_PRANA_ACCUM_EMPTY_TEXT } shouldBe true
            verify(actionProvider, never()).extractPrana()
        }
    }

    @Test
    fun `WHEN pet condition is not ok THEN notify`() {
        runBlocking {

            val eventBus = EventBus()

            val testState = HeroState(
                healthPercent = 50,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = false,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            val events = mutableListOf<BotEvent>()
            BotScope.launch {
                eventBus.botEventFlow.onEach {
                    events.add(it)
                }.collect()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            events.any { it.message == Constants.BOT_EVENT_PET_BAD_TEXT } shouldBe true
        }
    }

    @Test
    fun `WHEN pet condition is not ok and money enough to heal THEN notify`() {
        runBlocking {

            val eventBus = EventBus()

            val testState = HeroState(
                healthPercent = 50,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = false,
                moneyForPetHeal = 500
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            val events = mutableListOf<BotEvent>()
            BotScope.launch {
                eventBus.botEventFlow.onEach {
                    events.add(it)
                }.collect()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            events.any { it.message == Constants.BOT_EVENT_PET_BAD_TEXT } shouldBe true
            events.any { it.message == Constants.BOT_EVENT_PET_BAD_CAN_HEAL_TEXT } shouldBe true
        }
    }

    @Test
    fun `WHEN hero is dead THEN try resurrect`() {

        val eventBus = EventBus()

        actionProvider.stub {
            doNothing().on { resurrect() }
        }

        runBlocking {
            val testState = HeroState(
                healthPercent = 0,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            val events = mutableListOf<BotEvent>()
            BotScope.launch {
                eventBus.botEventFlow.onEach {
                    events.add(it)
                }.collect()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            events.any { it.message == Constants.BOT_EVENT_HERO_DEAD_TEXT } shouldBe true
            verify(actionProvider, atLeast(1)).resurrect()
        }
    }

    @Test
    fun `WHEN hero is dead and resurrect failed THEN call event`() {

        val eventBus = EventBus()

        actionProvider.stub {
            on { resurrect() } doThrow RuntimeException()
        }

        runBlocking {
            val testState = HeroState(
                healthPercent = 0,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            val events = mutableListOf<BotEvent>()
            BotScope.launch {
                eventBus.botEventFlow.onEach {
                    events.add(it)
                }.collect()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            events.any { it.message == Constants.BOT_EVENT_HERO_DEAD_TEXT } shouldBe true
            events.any { it.message == Constants.BOT_EVENT_FAILED_RESURRECT_HERO_TEXT } shouldBe true
            verify(actionProvider, atLeast(1)).resurrect()
        }
    }

    @Test
    fun `WHEN health ok THEN do nothing`() {

        val eventBus = EventBus()

        runBlocking {
            val testState = HeroState(
                healthPercent = 50,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            verify(actionProvider, never()).makeGood()
        }
    }

    @Test
    fun `WHEN health not ok THEN try heal`() {

        val eventBus = EventBus()

        runBlocking {
            val testState = HeroState(
                healthPercent = 10,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            verify(actionProvider, atLeast(1)).makeGood()
        }
    }

    @Test
    fun `WHEN health not ok AND heal failed THEN notify about failure`() {

        val eventBus = EventBus()

        actionProvider.stub {
            on { makeGood() } doThrow RuntimeException()
        }

        runBlocking {
            val testState = HeroState(
                healthPercent = 10,
                money = 1000,
                pranaLevel = 50,
                pranaInAccumulator = 100,
                isPetOk = true,
                moneyForPetHeal = 0
            )

            val bot = GodvilleBot(eventBus, actionProvider)
            BotScope.launch {
                bot.wait()
            }

            val events = mutableListOf<BotEvent>()
            NotificationsScope.launch {
                eventBus.botEventFlow.onEach { events.add(it) }.collect()
            }

            eventBus.emitHeroState(testState)
            delay(waitDelay)
            bot.close()

            verify(actionProvider, atLeast(1)).makeGood()
            events.any { it.message == Constants.BOT_EVENT_HEAL_FAILED_TEXT } shouldBe true
        }
    }
}