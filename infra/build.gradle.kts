import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.6.21"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.noarg") version "1.6.21"
}

repositories{
    mavenCentral()
}

group = "fr.sacane.jmanager"
version = "1.0-SNAPSHOT"
dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web:3.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.1.3")
    implementation("com.ToxicBakery.library.bcrypt:bcrypt:1.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.5.0")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security:3.1.3")
    implementation(project(mapOf("path" to ":domain")))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.3")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("com.h2database:h2")
}


tasks {
    compileJava{
        sourceCompatibility= "17"
        targetCompatibility = "17"
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    shadowJar {
        mergeServiceFiles()
        archiveBaseName.set("Jmanager")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
        destinationDirectory.set(file("$rootDir/executables"))
        append("META-INF/spring.handlers")
        append("META-INF/spring.schemas")
        append("META-INF/spring.tooling")
        transform(
            PropertiesFileTransformer().apply {
                paths = mutableListOf("META-INF/spring.factories")
                mergeStrategy = "append"
            }
        )
    }
    jar{
        manifest {
            attributes["Main-Class"] = "fr.sacane.jmanager.infrastructure.JmanagerApplicationKt"
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    processResources {
        mustRunAfter(":client:bundle")
    }
}
