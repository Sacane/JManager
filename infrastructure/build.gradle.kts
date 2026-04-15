import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("jmanager.kotlin-conventions")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("plugin.spring") version "2.0.0"
    kotlin("plugin.noarg") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
}

noArg {
    annotation("jakarta.persistence.Entity")
}

val springBootVersion = "3.4.0"
val flywayVersion = "11.14.1"
val testcontainersVersion = "1.21.3"

dependencies {
    implementation(project(":domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:${flywayVersion}")
    implementation("org.flywaydb:flyway-database-postgresql:${flywayVersion}")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:${flywayVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:${springBootVersion}")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:junit-jupiter:${testcontainersVersion}")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks.test {
    jvmArgs(
        "-Xmx512m",
        "-XX:MaxMetaspaceSize=256m"
    )
    systemProperty("testcontainers.reuse.enable", "true")
}

// Disable Spring Boot bootJar — this is a library module, not a bootable app
tasks.named("bootJar") {
    enabled = false
}
tasks.named<Jar>("jar") {
    enabled = true
}
