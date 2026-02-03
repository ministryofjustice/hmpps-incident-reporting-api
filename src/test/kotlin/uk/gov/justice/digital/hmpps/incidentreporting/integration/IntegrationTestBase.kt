package uk.gov.justice.digital.hmpps.incidentreporting.integration

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.incidentreporting.config.PostgresTestContainer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@ActiveProfiles("test")
abstract class IntegrationTestBase {

  companion object {
    // All tests should use a frozen instant for “now”: 12:34:56 on 5 December 2023 in London
    val zoneId: ZoneId = ZoneId.of("Europe/London")
    val clock: Clock = Clock.fixed(
      Instant.parse("2023-12-05T12:34:56.123456+00:00"),
      zoneId,
    )
    val now: LocalDateTime = LocalDateTime.now(clock)
    val today: LocalDate = now.toLocalDate()

    private val postgresInstance = PostgresTestContainer.instance

    @Suppress("unused")
    @JvmStatic
    @DynamicPropertySource
    fun postgresProperties(registry: DynamicPropertyRegistry) {
      postgresInstance?.let { PostgresTestContainer.setupProperties(postgresInstance, registry) }
    }
  }

  protected fun getResource(name: String): String {
    return this::class.java.getResource(name)?.readText()
      ?: throw NotImplementedError("test resource $name not defined")
  }
}
