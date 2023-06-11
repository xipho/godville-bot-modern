package ru.xipho.godvillebotmodern.bot.async

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ForkJoinPool
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object NotificationScope : CoroutineScope {

    private val logger = LoggerFactory.getLogger(NotificationScope::class.java)

    override val coroutineContext = EmptyCoroutineContext +
            ForkJoinPool.commonPool().asCoroutineDispatcher() +
            CoroutineExceptionHandler { context, exception ->
                logger.error(context.toString(), exception)
            } + CoroutineName("Notification")
}

object BotScope : CoroutineScope {

    private val logger = LoggerFactory.getLogger(BotScope::class.java)

    override val coroutineContext = EmptyCoroutineContext +
            ForkJoinPool.commonPool().asCoroutineDispatcher() +
            CoroutineExceptionHandler { context, exception ->
                logger.error(context.toString(), exception)
            } + CoroutineName("Bot")
}

class CustomClosableScope(
    scopeName: String = "CustomClosableScope"
): CoroutineScope, AutoCloseable {

    private val job = Job()

    override val coroutineContext: CoroutineContext = job + Dispatchers.Default + CoroutineName(scopeName)

    override fun close() {
        job.cancel()
    }
}

object FlowScope: CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default + CoroutineName("FlowScope")
}

object NotificationsScope: CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default + CoroutineName("NotificationScope")
}