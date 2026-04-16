plugins {
    id("jmanager.kotlin-conventions")
}

val bcryptVersion: String = "0.10.2"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("at.favre.lib", "bcrypt", bcryptVersion)

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
}

tasks.test {
    jvmArgs("-Xmx256m")
}