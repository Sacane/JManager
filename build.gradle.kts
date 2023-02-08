import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
	war
	kotlin("jvm") version "1.6.21"
}
group = "fr.sacane.jmanager"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
allprojects{
	repositories {
		mavenCentral()
	}
	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = "17"
		}
	}
	tasks.withType<Test> {
		useJUnitPlatform()
	}
}


