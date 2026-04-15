import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("jmanager.kotlin-conventions")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("plugin.spring") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

val springBootVersion = "3.4.0"
val restAssuredVersion = "5.5.0"
val jwtVersion = "0.12.6"
val testcontainersVersion = "1.21.3"
val mockitoVersion = "5.1.0"

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-security:${springBootVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("io.jsonwebtoken:jjwt-api:${jwtVersion}")
    implementation("io.jsonwebtoken:jjwt-impl:${jwtVersion}")
    implementation("com.ToxicBakery.library.bcrypt:bcrypt:1.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:11.14.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.14.1")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jwtVersion}")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.14.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:${springBootVersion}")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured:${restAssuredVersion}")
    testImplementation("io.rest-assured:kotlin-extensions:${restAssuredVersion}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${mockitoVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testcontainersVersion}")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")
}

tasks.test {
    jvmArgs(
        "-Xmx512m",
        "-XX:MaxMetaspaceSize=256m"
    )
    systemProperty("testcontainers.reuse.enable", "true")
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
        setProperty("zip64", true)
    }
    jar {
        manifest {
            attributes["Main-Class"] = "fr.sacane.jmanager.application.JmanagerApplicationKt"
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    processResources {
        mustRunAfter(":client:bundle")
    }
}
