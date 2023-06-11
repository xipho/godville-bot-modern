package ru.xipho.godvillebotmodern.bot.flows

data class HeroState(
    val healthPercent: Int,
    val money: Int,
    val pranaLevel: Int,
    val pranaInAccumulator: Int,
    val isPetOk: Boolean,
    val moneyForPetHeal: Int,
)
