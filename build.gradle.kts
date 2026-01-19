import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  kotlin("plugin.spring") version "2.3.0"
  idea
  id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.6.3")
  implementation("io.opentelemetry:opentelemetry-api:1.57.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.23.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:9.8.7")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.15")

  implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")

  implementation("com.pauldijou:jwt-core_2.11:5.0.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("javax.xml.bind:jaxb-api:2.3.1")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.41")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.testcontainers:testcontainers-localstack:2.0.3")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs = listOf(
      // cannot validate items within lists without this
      // cf. https://youtrack.jetbrains.com/issue/KT-67909/Resolve-inconsistencies-with-Java-in-emitting-JVM-type-annotations
      "-Xemit-jvm-type-annotations",
    )
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_24
  targetCompatibility = JavaVersion.VERSION_24
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
    compilerOptions.jvmTarget = JvmTarget.JVM_24
  }
}

allOpen {
  annotation("uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen")
}

openApi {
  customBootRun.args.set(listOf("--spring.profiles.active=dev,localstack"))
}
