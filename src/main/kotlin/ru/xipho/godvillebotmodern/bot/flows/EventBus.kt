package ru.xipho.godvillebotmodern.bot.flows

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.async.FlowScope
import ru.xipho.godvillebotmodern.bot.settings.BotSettings

class EventBus {

    private val logger = mu.KotlinLogging.logger { }

    private val _stateFlow = MutableStateFlow<HeroState?>(null)
    val stateFlow = _stateFlow as StateFlow<HeroState?>

    private val _settingsFlow = MutableStateFlow(BotSettings())
    val settingsFlow = _settingsFlow as StateFlow<BotSettings>

    private val _botEventFlow = MutableSharedFlow<BotEvent>()
    val botEventFlow = _botEventFlow as SharedFlow<BotEvent>

    fun emitHeroState(state: HeroState) = FlowScope.launch {
        logger.trace { "Emitting $state" }
        _stateFlow.emit(state)
        logger.trace { "Emitted $state" }
    }

    fun emitSettingsChange(botSettings: BotSettings) = FlowScope.launch {
        _settingsFlow.emit(botSettings)
    }

    fun emitBotEvent(event: BotEvent) = FlowScope.launch {
        logger.trace { "Emitting bot event $event" }
        _botEventFlow.emit(event)
        logger.trace { "Emitted bot event $event" }
    }
}