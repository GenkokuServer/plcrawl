import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.3.41"
    kotlin("plugin.serialization") version "1.3.50"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

group = "city.genkoku.plcrawl.finalize"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.1")
    implementation("org.eclipse.mylyn.github:org.eclipse.egit.github.core:2.1.5")
    implementation("com.auth0:java-jwt:3.3.0")
}

tasks.withType<Jar> {
    manifest {
        attributes(Pair("Main-Class", "city.genkoku.plcrawl.finalize.FinalizerKt"))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
