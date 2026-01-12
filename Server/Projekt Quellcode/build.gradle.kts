plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "me.toni"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-websockets:3.3.3")
    implementation("io.ktor:ktor-server-netty:3.3.3")
    implementation("org.json:json:20251224")
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "me.toni.MainKt"
        )
    }
}