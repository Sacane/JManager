
plugins{
    kotlin("jvm") version "1.6.21"
    jacoco
}

val bcryptVersion: String = "0.10.2"
group = "fr.sacane.jmanager"
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")


    implementation("at.favre.lib", "bcrypt", bcryptVersion)

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
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