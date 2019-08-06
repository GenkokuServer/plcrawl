import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.3.41"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

group = "city.genkoku.plcrawl.extract2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = "https://dl.bintray.com/nephyproject/dev")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.1")
    implementation("jp.nephy:jsonkt:5.0.0-eap-1")
}

tasks.withType<Jar> {
    manifest {
        attributes(Pair("Main-Class", "city.genkoku.plcrawl.extract2.ExtractorKt"))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
