import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    id("jmanager.kotlin-conventions")
    id("com.github.johnrengelman.shadow")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val springBootVersion: String by project
val restAssuredVersion: String by project
val testcontainersVersion: String by project
val mockitoVersion: String by project

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springBootVersion}")
    implementation("org.springframework.boot:spring-boot-starter-security:${springBootVersion}")
    implementation("org.springframework:spring-tx")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

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
