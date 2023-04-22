package ru.xipho.godvillebotmodern.bot.misc

import java.time.LocalDateTime

class SimpleRateLimiter(
    private val runsPerHour: Int = 3,
    private val runsPerFiveMinutes: Int = 3
) {
    private val runTaskPerHourCounter: MutableList<LocalDateTime> = mutableListOf()
    private val runTaskPerFiveMinutesCounter: MutableList<LocalDateTime> = mutableListOf()

    fun doRateLimited(action: () -> Unit) {
        val actionTime = LocalDateTime.now()
        if (isActionPerHourAvailable && isActionPerFiveMinutesAvailable) {
            action()
            runTaskPerHourCounter.add(actionTime)
            runTaskPerFiveMinutesCounter.add(actionTime)
        }
    }

    private val isActionPerHourAvailable: Boolean
        get() {
            val safeTime = LocalDateTime.now().minusHours(1)
            runTaskPerHourCounter.removeIf { it.isBefore(safeTime) }
            return runTaskPerHourCounter.size < runsPerHour
        }

    private val isActionPerFiveMinutesAvailable: Boolean
        get() {
            val safeTime = LocalDateTime.now().minusMinutes(5)
            runTaskPerFiveMinutesCounter.removeIf { it.isBefore(safeTime) }
            return runTaskPerFiveMinutesCounter.size < runsPerFiveMinutes
        }
}