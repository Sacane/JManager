plugins{
    war
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
}

group = "fr.sacane.jmanager"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

