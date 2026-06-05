import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
	java
	checkstyle
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("net.ltgt.errorprone") version "5.1.0"
	id("net.ltgt.nullaway") version "3.0.0"
	id("org.openapi.generator") version "7.22.0"
}

group = "com.demo"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2025.1.1"

dependencies {
	compileOnly("org.jspecify:jspecify:1.0.0")
	implementation("org.springframework.boot:spring-boot-micrometer-tracing-brave")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("io.micrometer:micrometer-tracing-bridge-brave")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
	
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("io.micrometer:micrometer-registry-cloudwatch2")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")

	errorprone("com.google.errorprone:error_prone_core:2.49.0")
	errorprone("com.uber.nullaway:nullaway:0.13.4")

	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
	testImplementation("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

checkstyle {
	toolVersion = "13.5.0"
	config = resources.text.fromArchiveEntry(
		configurations.checkstyle.get().filter { it.name.startsWith("checkstyle-") }.singleFile,
		"google_checks.xml",
	)
	maxWarnings = 0
}

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set(layout.projectDirectory.file("openapi/openapi.yaml").asFile.absolutePath)
	outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
	apiPackage.set("com.demo.skills.api")
	modelPackage.set("com.demo.skills.api.model")
	configOptions.set(
		mapOf(
			"annotationLibrary" to "none",
			"documentationProvider" to "none",
			"interfaceOnly" to "true",
			"useSpringBoot3" to "true",
			"useTags" to "true",
			"openApiNullable" to "false",
			"dateLibrary" to "java8",
		),
	)
}

openApiValidate {
	inputSpec.set(layout.projectDirectory.file("openapi/openapi.yaml").asFile.absolutePath)
}

sourceSets {
	main {
		java {
			srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
		}
	}
}

nullaway {
	onlyNullMarked = true
	jspecifyMode = true
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone.nullaway {
		error()
		checkOptionalEmptiness.set(true)
		handleTestAssertionLibraries.set(true)
	}
}

tasks.withType<Checkstyle>().configureEach {
	exclude("**/build/generated/**")
	exclude("**/generated/openapi/**")
	exclude("com/demo/skills/api/**")
}

tasks.named("compileJava") {
	dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("check") {
	dependsOn(tasks.named("openApiValidate"))
}

tasks.withType<Test> {
	useJUnitPlatform()
}
