import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
    kotlin("plugin.noarg") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    jacoco
}

noArg {
    annotation("jakarta.persistence.Entity")
}

repositories{
    mavenCentral()
}

group = "fr.sacane.jmanager"
version = "1.0"
val springBootVersion = "3.4.0"
val restAssuredVersion = "5.5.0"
val jwtVersion = "0.12.6"
val flywayVersion = "11.14.1"
val testcontainersVersion = "1.21.3"

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}")
    implementation("com.ToxicBakery.library.bcrypt:bcrypt:1.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.5.0")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-security:${springBootVersion}")
    implementation("org.flywaydb:flyway-core:${flywayVersion}")
    implementation(project(mapOf("path" to ":domain")))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("io.jsonwebtoken:jjwt-api:${jwtVersion}")
    implementation("io.jsonwebtoken:jjwt-impl:${jwtVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:${springBootVersion}")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured:${restAssuredVersion}")
    testImplementation("io.rest-assured:kotlin-extensions:${restAssuredVersion}")

    // Testcontainers for PostgreSQL
    testImplementation("org.testcontainers:junit-jupiter:${testcontainersVersion}")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jwtVersion}")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:${flywayVersion}")
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)  // Important pour SonarQube
        html.required.set(false)
        csv.required.set(false)
    }
}

tasks {
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
        from("infra/src/main/resources/application.properties") {
            into("executables")
        }
        setProperty("zip64", true)
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
