package ru.xipho.godvillebotmodern.bot.api

interface HeroActionProvider: AutoCloseable {
    fun getHealth(): Int
    fun getHealthPercent(): Int
    fun getMoney(): Int
    fun getNeededForPetResurrectMoney(): Int
    fun getAccum(): Int
    fun resurrect()
    fun getCurrentPrana(): Int
    fun makeGood()
    @Suppress("unused")
    fun makeBad()
    fun extractPrana(): Boolean
    fun isPetOk(): Boolean
    fun useFirstPranaFromInventoryIfHave()
}