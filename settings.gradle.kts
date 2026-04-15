pluginManagement {
    includeBuild("build-logic")
    val props = java.util.Properties().also {
        it.load(file("gradle.properties").reader())
    }
    plugins {
        id("org.springframework.boot")              version props["springBootVersion"]              as String
        id("io.spring.dependency-management")       version props["springDependencyManagementVersion"] as String
        id("com.github.johnrengelman.shadow")        version props["shadowVersion"]                  as String
        id("org.jetbrains.kotlin.plugin.spring")     version props["kotlinVersion"]                  as String
        id("org.jetbrains.kotlin.plugin.noarg")      version props["kotlinVersion"]                  as String
        id("org.jetbrains.kotlin.plugin.jpa")        version props["kotlinVersion"]                  as String
        id("org.jetbrains.kotlin.plugin.serialization") version props["kotlinVersion"]               as String
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Jmanager"
include(":domain", ":infrastructure", ":application", ":client")