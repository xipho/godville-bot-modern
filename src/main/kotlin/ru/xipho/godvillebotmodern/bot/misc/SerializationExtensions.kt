package ru.xipho.godvillebotmodern.bot.misc

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class GsonExcludeField

object AnnotatedWithExcludeFieldExclusionStrategy: ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return f.getAnnotation(GsonExcludeField::class.java) != null
    }
    override fun shouldSkipClass(clazz: Class<*>?): Boolean = false
}
