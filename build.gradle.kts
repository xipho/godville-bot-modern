import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
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
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.seleniumhq.selenium:selenium-java:4.9.0")
	implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.8.3")
	implementation("org.seleniumhq.selenium:selenium-chromium-driver:4.8.3")
	implementation("org.seleniumhq.selenium:selenium-remote-driver:4.8.3")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.squareup.okhttp3:okhttp:4.9.3")
	implementation("org.slf4j:slf4j-api:2.0.5")
	implementation("com.github.pengrad:java-telegram-bot-api:6.5.0")
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
	runtimeOnly("ch.qos.logback:logback-classic:1.4.7")

	testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
	testImplementation("io.kotest:kotest-assertions-core:5.6.0")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
	testImplementation("org.mockito:mockito-core:5.3.1")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Jar> {
	// Иначе вы получите ошибку "No main manifest attribute"
	manifest {
		attributes ["Main-Class"] = "ru.xipho.godvillebotmodern.GodvilleBotModernApplicationKt"
	}
	// Чтобы избежать ошибки дублирования стратегии обработки
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	// Чтобы добавить все зависимости, иначе ошибка "NoClassDefFoundError"
	from (sourceSets.main.get ().output)
	dependsOn (configurations.runtimeClasspath)
	from ( {
		configurations.runtimeClasspath.get ().filter { it.name.endsWith ("jar") }.map { zipTree (it) }
	})
}

tasks.withType<Test> {
	useJUnitPlatform()
}

//tasks.withType<Test>().configureEach {
//	useJUnitPlatform()
//}
