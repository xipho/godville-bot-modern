package ru.xipho.godvillebotmodern

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class GodvilleBotModernApplication

fun main(args: Array<String>) {
	runApplication<GodvilleBotModernApplication>(*args)
}
