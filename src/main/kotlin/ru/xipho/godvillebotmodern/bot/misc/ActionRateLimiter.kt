package ru.xipho.godvillebotmodern.bot.misc


import java.time.Duration
import java.time.LocalDateTime

class ActionRateLimiter(
    val interval: Duration,
    val allowedActions: Int
) {
    private var lastActionTime: LocalDateTime = LocalDateTime.MIN

    fun doRateLimited(action: () -> Unit) {
        val safePeriod = interval.seconds / allowedActions
        val safeTime = lastActionTime.plusSeconds(safePeriod)
        if (lastActionTime.isBefore(safeTime)) {
            val now = LocalDateTime.now()
            action.invoke()
            lastActionTime = now
        }
    }
}