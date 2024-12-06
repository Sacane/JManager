import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	kotlin("jvm") version "2.0.0"
	id("com.github.johnrengelman.shadow") version "7.0.0"
	id("org.sonarqube") version "5.0.0.4638"
	jacoco
}


group = "fr.sacane"
java.sourceCompatibility = JavaVersion.toVersion("21")
dependencies{
	implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.5.0")
}
allprojects{
	repositories {
		mavenCentral()
	}
	tasks.withType<Test> {
		useJUnitPlatform()
	}
}

sonar {
	properties {
		property("sonar.projectKey", "Sacane_JManager_aa4d0a52-73c4-4b64-a0e2-6f5565902347")
		property("sonar.projectName", "JManager")
		property("sonar.coverage.jacoco.xmlReportPaths",
			listOf(
				"$rootDir/domain/build/reports/jacoco/test/jacocoTestReport.xml",
				"$rootDir/infra/build/reports/jacoco/test/jacocoTestReport.xml"
			).joinToString(",")
		)
	}
}


tasks.test {
	finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
	dependsOn(tasks.test) // tests are required to run before generating the report
}
tasks.register("jacocoRootReport", JacocoReport::class) {
	dependsOn(subprojects.mapNotNull { it.tasks.findByName("test") }) // Utilisation de mapNotNull pour éviter les erreurs
	description = "Generates an aggregate report from all subprojects"
	group = "verification"
	executionData.setFrom(
		fileTree(project.rootDir).apply {
			include("**/build/jacoco/test.exec")  // Chemin où sont générés les fichiers exec de JaCoCo
		}
	)

	// Récupération des sources et des classes à partir des sous-projets
	sourceDirectories.setFrom(
		files(subprojects.mapNotNull { it.extensions.findByType<SourceSetContainer>()?.getByName("main")?.allSource?.srcDirs })
	)
	classDirectories.setFrom(
		files(subprojects.mapNotNull { it.extensions.findByType<SourceSetContainer>()?.getByName("main")?.output })
	)

	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}
tasks {
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
