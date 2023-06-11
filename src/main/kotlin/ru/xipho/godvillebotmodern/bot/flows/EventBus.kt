package ru.xipho.godvillebotmodern.bot.flows

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.xipho.godvillebotmodern.bot.api.events.BotEvent
import ru.xipho.godvillebotmodern.bot.async.FlowScope
import ru.xipho.godvillebotmodern.bot.settings.BotSettings

object EventBus {

    private val _stateFlow = MutableStateFlow<HeroState?>(null)
    val stateFlow = _stateFlow as StateFlow<HeroState?>

    private val _settingsFlow = MutableStateFlow(BotSettings())
    val settingsFlow = _settingsFlow as StateFlow<BotSettings>

    private val _botEventFlow = MutableSharedFlow<BotEvent>()
    val botEventFlow = _botEventFlow as SharedFlow<BotEvent>

    fun emitHeroState(state: HeroState) = FlowScope.launch {
        _stateFlow.emit(state)
    }

    fun emitSettingsChange(botSettings: BotSettings) = FlowScope.launch {
        _settingsFlow.emit(botSettings)
    }

    fun emitBotEvent(event: BotEvent) = FlowScope.launch {
        _botEventFlow.emit(event)
    }
}