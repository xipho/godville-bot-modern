package ru.xipho.godvillebotmodern.bot.misc

import java.time.LocalDateTime

class SimpleRateLimiter(
    private val runPerHours: Int = 3
) {
    private val runTaskPerHourCounter: MutableList<LocalDateTime> = mutableListOf()
    private val runTaskPerMinuteCounter: MutableList<LocalDateTime> = mutableListOf()

    fun doRateLimited(action: () -> Unit) {
        val actionTime = LocalDateTime.now()
        if (isActionPerHourAvailable && isActionPerMinuteAvailable) {
            action()
            runTaskPerHourCounter.add(actionTime)
            runTaskPerMinuteCounter.add(actionTime)
        }
    }

    private val isActionPerHourAvailable: Boolean
        get() {
            val safeTime = LocalDateTime.now().minusHours(1)
            runTaskPerHourCounter.removeIf { it.isBefore(safeTime) }
            return runTaskPerHourCounter.size < runPerHours
        }

    private val isActionPerMinuteAvailable: Boolean
        get() {
            val safeTime = LocalDateTime.now().minusMinutes(5)
            runTaskPerMinuteCounter.removeIf { it.isBefore(safeTime) }
            return runTaskPerMinuteCounter.size < 3
        }
}