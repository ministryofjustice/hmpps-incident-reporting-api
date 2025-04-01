package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.TypeFamily
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase

@DisplayName("Compare the result of migrations to in-built constant enumerations")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConstantsTableTest : IntegrationTestBase() {
  @Autowired
  lateinit var jdbcClient: JdbcClient

  private fun listAllConstants(query: String) = jdbcClient
    .sql("$query ORDER BY sequence")
    .query(ColumnMapRowMapper())
    .list()

  @Test
  fun `prisoner outcome table`() {
    val expected = PrisonerOutcome.entries.map {
      mapOf(
        "code" to it.name,
        "description" to it.description,
      )
    }
    val actual = listAllConstants(
      // language=postgresql
      """
      SELECT code, description FROM constant_prisoner_outcome
      """,
    )
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `prisoner role table`() {
    val expected = PrisonerRole.entries.map {
      mapOf(
        "code" to it.name,
        "description" to it.description,
      )
    }
    val actual = listAllConstants(
      // language=postgresql
      """
      SELECT code, description FROM constant_prisoner_role
      """,
    )
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `staff role table`() {
    val expected = StaffRole.entries.map {
      mapOf(
        "code" to it.name,
        "description" to it.description,
      )
    }
    val actual = listAllConstants(
      // language=postgresql
      """
      SELECT code, description FROM constant_staff_role
      """,
    )
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `status table`() {
    val expected = Status.entries.map {
      mapOf(
        "code" to it.name,
        "description" to it.description,
      )
    }
    val actual = listAllConstants(
      // language=postgresql
      """
      SELECT code, description FROM constant_status
      """,
    )
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `type table`() {
    val expected = Type.entries.map {
      mapOf(
        "code" to it.name,
        "description" to it.description,
        "active" to it.active,
      )
    }
    val actual = listAllConstants(
      // language=postgresql
      """
      SELECT code, description, active FROM constant_type
      """,
    )
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `type family table`() {
    val expected = TypeFamily.entries.map {
      mapOf(
        "code" to it.name,
        "description" to it.description,
      )
    }
    val actual = listAllConstants(
      // language=postgresql
      """
      SELECT code, description FROM constant_type_family
      """,
    )
    assertThat(actual).isEqualTo(expected)
  }
}
