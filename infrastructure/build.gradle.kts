import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("jmanager.kotlin-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
    kotlin("plugin.noarg")
    kotlin("plugin.jpa")
}

noArg {
    annotation("jakarta.persistence.Entity")
}

val springBootVersion: String by project
val flywayVersion: String by project
val jwtVersion: String by project
val testcontainersVersion: String by project
val mockitoVersion: String by project

dependencies {
    implementation(project(":domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springBootVersion}")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:${flywayVersion}")
    implementation("org.flywaydb:flyway-database-postgresql:${flywayVersion}")
    implementation("io.jsonwebtoken:jjwt-api:${jwtVersion}")
    implementation("io.jsonwebtoken:jjwt-impl:${jwtVersion}")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jwtVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springBootVersion}")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:${springBootVersion}")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:junit-jupiter:${testcontainersVersion}")
    testImplementation("org.testcontainers:postgresql:${testcontainersVersion}")
    testImplementation("org.testcontainers:testcontainers:${testcontainersVersion}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${mockitoVersion}")
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
