import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	kotlin("jvm") version "1.6.21"
	id("com.github.johnrengelman.shadow") version "7.0.0"
	id("org.sonarqube") version "5.0.0.4638"
	jacoco
}

sonar {
	properties {
		property("sonar.projectKey", "Sacane_JManager_aa4d0a52-73c4-4b64-a0e2-6f5565902347")
		property("sonar.projectName", "JManager")
		property("sonar.coverage.jacoco.xmlReportPaths",
			listOf(
				"$projectDir/domain/build/reports/jacoco/test/jacocoTestReport.xml",
				"$projectDir/infra/build/reports/jacoco/test/jacocoTestReport.xml"
			).joinToString(",")
		)
	}
}

group = "fr.sacane"
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

tasks.test {
	finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
	dependsOn(tasks.test) // tests are required to run before generating the report
}
tasks.register("jacocoRootReport", JacocoReport::class) {
	dependsOn(subprojects.map { it.tasks.named("test") })

	val jacocoTestReportFiles = subprojects.map {
		it.tasks.named("jacocoTestReport").get().outputs.files.singleFile
	}

	executionData.setFrom(jacocoTestReportFiles)

	sourceDirectories.setFrom(files(subprojects.flatMap { it.sourceSets["main"].allSource.srcDirs }))
	classDirectories.setFrom(files(subprojects.flatMap { it.sourceSets["main"].output }))

	reports {
		xml.required.set(true)
		html.required.set(true)
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
		dependsOn(":domain:assemble")
		dependsOn(":infra:assemble")
		dependsOn(":infra:shadowJar")
	}
}
