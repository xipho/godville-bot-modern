import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.5"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.22"
}

group = "ru.xipho"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/release") }
	gradlePluginPortal()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-quartz")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.seleniumhq.selenium:selenium-java:4.9.0")
	implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.8.3")
	implementation("org.seleniumhq.selenium:selenium-chromium-driver:4.8.3")
	implementation("org.seleniumhq.selenium:selenium-remote-driver:4.8.3")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.squareup.okhttp3:okhttp:4.9.3")
	implementation("org.slf4j:slf4j-api:2.0.5")
	implementation("com.github.pengrad:java-telegram-bot-api:6.5.0")
	implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
	runtimeOnly("ch.qos.logback:logback-classic:1.4.6")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
