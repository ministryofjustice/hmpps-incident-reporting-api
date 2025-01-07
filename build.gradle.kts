import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.2"
  kotlin("plugin.jpa") version "2.1.0"
  kotlin("plugin.spring") version "2.1.0"
  idea
  id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.2")
  implementation("io.opentelemetry:opentelemetry-api:1.45.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.11.0")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.4")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.1")

  implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

  implementation("com.pauldijou:jwt-core_2.11:5.0.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.1.1")
  testImplementation("org.wiremock:wiremock-standalone:3.10.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.24")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.testcontainers:localstack:1.20.4")
  testImplementation("org.testcontainers:postgresql:1.20.4")
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    freeCompilerArgs = listOf(
      // cannot validate items within lists without this
      // cf. https://youtrack.jetbrains.com/issue/KT-67909/Resolve-inconsistencies-with-Java-in-emitting-JVM-type-annotations
      "-Xemit-jvm-type-annotations",
    )
  }
}

tasks {
  register<PortForwardRDSTask>("portForwardRDS") {
    namespacePrefix = "hmpps-incident-reporting"
  }

  register<PortForwardRedisTask>("portForwardRedis") {
    namespacePrefix = "hmpps-incident-reporting"
  }

  register<RevealSecretsTask>("revealSecrets") {
    namespacePrefix = "hmpps-incident-reporting"
  }

  withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
  }
}

allOpen {
  annotation("uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen")
}

openApi {
  customBootRun.args.set(listOf("--spring.profiles.active=dev,localstack"))
}
