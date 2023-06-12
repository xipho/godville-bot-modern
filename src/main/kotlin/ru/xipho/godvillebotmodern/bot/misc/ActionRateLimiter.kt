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
        val now = LocalDateTime.now()
        val safeTime = now.minusSeconds(safePeriod)
        if (lastActionTime.isBefore(safeTime)) {
            action.invoke()
            lastActionTime = now
        }
    }
}