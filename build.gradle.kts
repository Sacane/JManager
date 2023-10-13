import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile



plugins {
	kotlin("jvm") version "1.6.21"
	id("com.github.johnrengelman.shadow") version "7.0.0"
}
group = "fr.sacane.jmanager"
java.sourceCompatibility = JavaVersion.VERSION_17
dependencies{
	implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.5.0")
}
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

tasks {
	compileJava{
		sourceCompatibility= "17"
		targetCompatibility = "17"
	}
	compileKotlin {
		kotlinOptions {
			jvmTarget = "17"
		}
	}
	clean {
		dependsOn(":client:clean")
		delete(file("${project.projectDir}/executables"))
	}
	assemble {
		actions.clear()
		dependsOn(":client:bundle")
		dependsOn(":domain:assemble")
		dependsOn(":infra:assemble")
	}
	build {
		actions.clear()
		dependsOn(":client:bundle")
		dependsOn(":infra:shadowJar")
	}
}
