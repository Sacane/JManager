
plugins{
    war
    kotlin("jvm") version "1.6.21"
}

group = "fr.sacane.jmanager"
version = "1.6.21"
java.sourceCompatibility = JavaVersion.VERSION_17
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
}

