plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.liquibase.gradle") version "2.2.0"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.devgate"
version = "1.0.0"
description = "User service"

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

val mockitoAgent: Configuration = configurations.create("mockitoAgent")

dependencies {
//	starters
	implementation("org.springframework.boot:spring-boot-starter-data-rest")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-liquibase")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	testImplementation("org.springframework.boot:spring-boot-starter-data-rest-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
	testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")

//	docker
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")

//	database
	runtimeOnly("org.postgresql:postgresql")

//	migrations
	implementation("org.liquibase:liquibase-core")
	liquibaseRuntime("org.liquibase:liquibase-core:5.0")
	liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:2.1.1")
	liquibaseRuntime("org.yaml:snakeyaml:2.0")
	liquibaseRuntime("org.postgresql:postgresql")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

//	test dependencies
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.mockito:mockito-core:5.21.0")
	mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks {
	withType<Test> {
		useJUnitPlatform()
	}

	test {
		jvmArgs.add("-javaagent:${mockitoAgent.asPath}")
	}

	bootJar {
		archiveFileName.set("${project.group}.${project.name}.jar")
	}
}