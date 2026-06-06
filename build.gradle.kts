import java.math.BigDecimal
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
	java
	checkstyle
	jacoco
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
extra["springCloudAwsVersion"] = "4.0.2"
extra["testcontainersVersion"] = "2.0.5"

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
	implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")
	
	compileOnly("org.projectlombok:lombok")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.mapstruct:mapstruct:1.6.3")
	runtimeOnly("io.micrometer:micrometer-registry-cloudwatch2")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	errorprone("com.google.errorprone:error_prone_core:2.49.0")
	errorprone("com.uber.nullaway:nullaway:0.13.4")

	testImplementation(platform("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}"))
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
	testImplementation("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-postgresql:${property("testcontainersVersion")}")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter:${property("testcontainersVersion")}")
	testCompileOnly("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testAnnotationProcessor("org.projectlombok:lombok")
	testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
		mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${property("springCloudAwsVersion")}")
		mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
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

jacoco {
	toolVersion = "0.8.14"
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

val generatedOpenApiSources = layout.buildDirectory.dir("generated/openapi/src/main/java")

nullaway {
	onlyNullMarked = true
	jspecifyMode = true
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone.excludedPaths.set(".*/build/generated/.*")
	options.errorprone.nullaway {
		error()
		checkOptionalEmptiness.set(true)
		handleTestAssertionLibraries.set(true)
	}
}

tasks.withType<Checkstyle>().configureEach {
	// Exclude OpenAPI Generator output; the contract is validated by openApiValidate,
	// and generated code does not consistently follow this repo's Checkstyle rules.
	val generatedOpenApiPath = generatedOpenApiSources.get().asFile.toPath()
	source =
		source.matching {
			exclude { element -> element.file.toPath().startsWith(generatedOpenApiPath) }
		}
}

tasks.named("compileJava") {
	dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("check") {
	dependsOn(
		tasks.named("openApiValidate"),
		tasks.named("jacocoTestReport"),
		tasks.named("jacocoTestCoverageVerification"),
	)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.required.set(true)
		html.required.set(true)
	}

	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) {
					exclude("com/demo/skills/api/**")
				}
			},
		),
	)
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)

	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) {
					exclude("com/demo/skills/api/**")
				}
			},
		),
	)

	violationRules {
		rule {
			limit {
				minimum = BigDecimal("0.80")
			}
		}
	}
}
