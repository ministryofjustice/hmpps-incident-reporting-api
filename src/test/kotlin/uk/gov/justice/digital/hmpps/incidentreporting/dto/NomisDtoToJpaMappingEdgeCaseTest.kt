package uk.gov.justice.digital.hmpps.incidentreporting.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisCode
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaff
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStatus
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.toNewEntity
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.clock
import java.time.LocalDateTime

/**
 * Tests for edge cases when converting NOMIS DTOs to JPA entities.
 * At present, these are mostly field-by-field copies with some fields renamed.
 * NB: most conversions are already covered by resource and service tests.
 */
class NomisDtoToJpaMappingEdgeCaseTest {
  private val now = LocalDateTime.now(clock)

  @DisplayName("when creating a new report")
  @Nested
  inner class WhenCreatingNewReport {
    private val minimalReportDto = NomisReport(
      incidentId = 112414323,
      questionnaireId = 2124,
      title = "TITLE",
      description = "DESCRIPTION",
      prison = NomisCode("MDI", "Moorland (HMP)"),
      status = NomisStatus("AWAN", "Awaiting Analysis"),
      type = "SELF_HARM",
      lockedResponse = false,
      incidentDateTime = now,
      reportingStaff = NomisStaff("user1", 121, "John", "Smith"),
      reportedDateTime = now,
      staffParties = emptyList(),
      offenderParties = emptyList(),
      requirements = emptyList(),
      questions = emptyList(),
      history = emptyList(),
    )

    @Test
    fun `report details should be copied to new report and event`() {
      val reportDto = minimalReportDto.copy()

      val reportEntity = reportDto.toNewEntity(clock)
      assertThat(reportEntity.title).isEqualTo("TITLE")
      assertThat(reportEntity.description).isEqualTo("DESCRIPTION")
      assertThat(reportEntity.incidentNumber).isEqualTo("112414323")
      assertThat(reportEntity.prisonId).isEqualTo("MDI")
      assertThat(reportEntity.questionSetId).isEqualTo("2124")
      assertThat(reportEntity.assignedTo).isEqualTo("user1")
      assertThat(reportEntity.lastModifiedBy).isEqualTo("user1")
      assertThat(reportEntity.reportedBy).isEqualTo("user1")
      assertThat(reportEntity.getQuestions()).isEmpty()
      assertThat(reportEntity.history).isEmpty()
      assertThat(reportEntity.staffInvolved).isEmpty()
      assertThat(reportEntity.prisonersInvolved).isEmpty()
      assertThat(reportEntity.correctionRequests).isEmpty()
      assertThat(reportEntity.historyOfStatuses).hasSize(1)
      assertThat(reportEntity.historyOfStatuses).allSatisfy { status ->
        assertThat(status.status).isEqualTo(Status.AWAITING_ANALYSIS)
      }

      val eventEntity = reportEntity.event
      assertThat(eventEntity.title).isEqualTo("TITLE")
      assertThat(eventEntity.description).isEqualTo("DESCRIPTION")
      assertThat(eventEntity.eventId).isEqualTo("112414323")
      assertThat(eventEntity.prisonId).isEqualTo("MDI")
      assertThat(eventEntity.lastModifiedBy).isEqualTo("user1")
    }

    @Test
    fun `missing report title adopts a fallback value`() {
      val reportDto = minimalReportDto.copy(title = null)

      val reportEntity = reportDto.toNewEntity(clock)
      assertThat(reportEntity.title).isEqualTo("NO DETAILS GIVEN")
      assertThat(reportEntity.description).isEqualTo("DESCRIPTION")

      val eventEntity = reportEntity.event
      assertThat(eventEntity.title).isEqualTo("NO DETAILS GIVEN")
      assertThat(eventEntity.description).isEqualTo("DESCRIPTION")
    }

    @Test
    fun `missing report description adopts a fallback value`() {
      val reportDto = minimalReportDto.copy(description = null)

      val reportEntity = reportDto.toNewEntity(clock)
      assertThat(reportEntity.title).isEqualTo("TITLE")
      assertThat(reportEntity.description).isEqualTo("NO DETAILS GIVEN")

      val eventEntity = reportEntity.event
      assertThat(eventEntity.title).isEqualTo("TITLE")
      assertThat(eventEntity.description).isEqualTo("NO DETAILS GIVEN")
    }
  }

  @DisplayName("when updating an existing report")
  @Nested
  inner class WhenUpdatingExistingReport {
    // TODO
  }
}
