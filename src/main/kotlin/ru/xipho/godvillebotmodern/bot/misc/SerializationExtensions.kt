package ru.xipho.godvillebotmodern.bot.misc

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class GsonExcludeField

@Configuration
open class SerializationConfig {

    @Bean
    open fun objectMapper(): Gson = GsonBuilder()
        .setPrettyPrinting()
        .addSerializationExclusionStrategy(AnnotatedWithExcludeFieldExclusionStrategy)
        .create()
}


object AnnotatedWithExcludeFieldExclusionStrategy: ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return f.getAnnotation(GsonExcludeField::class.java) != null
    }
    override fun shouldSkipClass(clazz: Class<*>?): Boolean = false
}
