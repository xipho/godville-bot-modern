package ru.xipho.godvillebotmodern.bot.misc

import java.time.LocalDateTime

class SimpleRateLimiter(
    private val runsPerHour: Int = 3,
    private val runsPerFiveMinutes: Int = 3
) {
    private val runTaskPerHourCounter: MutableMap<String, MutableList<LocalDateTime>> = mutableMapOf()
    private val runTaskPerFiveMinutesCounter: MutableMap<String, MutableList<LocalDateTime>> = mutableMapOf()

    fun doRateLimited(id: String, action: () -> Unit) {
        val actionTime = LocalDateTime.now()
        if (isActionPerHourAvailable(id) && isActionPerFiveMinutesAvailable(id)) {
            action()
            runTaskPerHourCounter.computeIfAbsent(id) { mutableListOf() }.add(actionTime)
            runTaskPerFiveMinutesCounter.computeIfAbsent(id) { mutableListOf() }.add(actionTime)
        }
    }

    private fun isActionPerHourAvailable(id: String): Boolean {
        val safeTime = LocalDateTime.now().minusHours(1)
        return runTaskPerHourCounter[id]?.let { dateTimes ->
            dateTimes.removeIf { it.isBefore(safeTime) }
            dateTimes.size < runsPerHour
        } ?: true
    }

    private fun isActionPerFiveMinutesAvailable(id: String): Boolean {
        val safeTime = LocalDateTime.now().minusMinutes(5)
        return runTaskPerFiveMinutesCounter[id]?.let { localDateTimes ->
            localDateTimes.removeIf { it.isBefore(safeTime) }
            localDateTimes.size < runsPerFiveMinutes
        } ?: true
    }
}