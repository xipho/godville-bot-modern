package ru.xipho.godvillebotmodern.bot.async

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import java.util.concurrent.ForkJoinPool
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