package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import java.util.UUID

class PrisonerInvolvementTest {

  val report: Report = Mockito.mock(Report::class.java)

  @BeforeEach
  fun setUp() {
    Mockito.`when`(report.id).thenReturn(UUID.randomUUID())
    Mockito.`when`(report.reportReference).thenReturn("123456")
  }

  @Test
  fun `test prisoner involvement sorting`() {
    val involvement1 = PrisonerInvolvement(
      id = 1L,
      report = report,
      prisonerNumber = "A1234BC",
      prisonerRole = PrisonerRole.VICTIM,
      outcome = PrisonerOutcome.REMAND,
      comment = "Test comment 1",
    )
    val involvement2 = PrisonerInvolvement(
      id = null,
      report = report,
      prisonerNumber = "B2345CD",
      prisonerRole = PrisonerRole.PERPETRATOR,
      outcome = PrisonerOutcome.CONVICTED,
      comment = "Test comment 2",
    )
    val involvement3 = PrisonerInvolvement(
      id = null,
      report = report,
      prisonerNumber = "A1234BC",
      prisonerRole = PrisonerRole.VICTIM,
      outcome = null,
      comment = null,
    )

    val sortedSet = sortedSetOf(involvement2, involvement3, involvement1)

    assertThat(sortedSet.toList()).containsExactly(
      involvement3,
      involvement1,
      involvement2,
    )
  }
}
