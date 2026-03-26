import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	kotlin("jvm") version "2.0.0"
	id("com.github.johnrengelman.shadow") version "7.0.0"
	// id("org.sonarqube") version "5.0.0.4638"
	jacoco
}


group = "fr.sacane"
java.sourceCompatibility = JavaVersion.toVersion("21")

allprojects{
	group = rootProject.group
	version = rootProject.version

	repositories {
		mavenCentral()
	}
	tasks.withType<Test> {
		useJUnitPlatform()
	}
}

// sonar {
// 	properties {
// 		property("sonar.projectKey", "Sacane_JManager_bd0b693a-0a00-4752-98e2-4c1466482beb")
// 		property("sonar.projectName", "Jmanager")
// 		// Let Gradle subprojects (domain/infra) provide their own source sets.
// 		// Only declare frontend paths at root to avoid duplicate indexing.
// 		property("sonar.sources", "client")
// 		property("sonar.tests", "client/tests")
// 		property("sonar.exclusions", "**/node_modules/**,**/.nuxt/**,**/.output/**,**/dist/**,**/coverage/**,**/.pnpm-store/**")
// 		property("sonar.test.inclusions", "client/tests/**/*.spec.ts")
// 		property("sonar.coverage.jacoco.xmlReportPaths",
// 			listOf(
// 				"$rootDir/domain/build/reports/jacoco/test/jacocoTestReport.xml",
// 				"$rootDir/infra/build/reports/jacoco/test/jacocoTestReport.xml"
// 			).joinToString(",")
// 		)
// 		property("sonar.javascript.lcov.reportPaths", "$rootDir/client/coverage/lcov.info")
// 	}
// }


tasks.test {
	finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
	dependsOn(tasks.test) // tests are required to run before generating the report
}
tasks.register("jacocoRootReport", JacocoReport::class) {
	dependsOn(subprojects.mapNotNull { it.tasks.findByName("test") })
	description = "Generates an aggregate report from all subprojects"
	group = "verification"
	executionData.setFrom(
		fileTree(project.rootDir).apply {
			include("**/build/jacoco/test.exec")
		}
	)

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
