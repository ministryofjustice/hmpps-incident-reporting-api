package uk.gov.justice.digital.hmpps.incidentreporting.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisCode
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaff
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStatus
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.clock
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.now
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event

/**
 * Tests for edge cases when converting NOMIS DTOs to JPA entities.
 * At present, these are mostly field-by-field copies with some fields renamed.
 * NB: most conversions are already covered by resource and service tests.
 */
@DisplayName("Mapping NOMIS DTOs to JPA entities")
class NomisDtoToJpaMappingEdgeCaseTest {
  private val minimalReportDto = NomisReport(
    incidentId = 112414323,
    questionnaireId = 2124,
    title = "TITLE",
    description = "DESCRIPTION",
    prison = NomisCode("MDI", "Moorland (HMP)"),
    status = NomisStatus("AWAN", "Awaiting Analysis"),
    type = "FINDS1",
    lockedResponse = false,
    incidentDateTime = now.minusDays(1),
    reportingStaff = NomisStaff("user1", 121, "John", "Smith"),
    reportedDateTime = now.minusMinutes(5),
    createDateTime = now.minusMinutes(4),
    createdBy = "user1",
    staffParties = emptyList(),
    offenderParties = emptyList(),
    requirements = emptyList(),
    questions = emptyList(),
    history = emptyList(),
  )

  @DisplayName("when creating a new report")
  @Nested
  inner class WhenCreatingNewReport {
    @Test
    fun `report details should be copied to new report and event`() {
      val reportDto = minimalReportDto.copy()

      val reportEntity = Event.createReport(reportDto)
      assertThat(reportEntity.title).isEqualTo("TITLE")
      assertThat(reportEntity.description).isEqualTo("DESCRIPTION")
      assertThat(reportEntity.reportReference).isEqualTo("112414323")
      assertThat(reportEntity.location).isEqualTo("MDI")
      assertThat(reportEntity.questionSetId).isEqualTo("2124")
      assertThat(reportEntity.assignedTo).isEqualTo("user1")
      assertThat(reportEntity.modifiedBy).isEqualTo("user1")
      assertThat(reportEntity.reportedBy).isEqualTo("user1")
      assertThat(reportEntity.source).isEqualTo(InformationSource.NOMIS)
      assertThat(reportEntity.modifiedIn).isEqualTo(InformationSource.NOMIS)
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
      assertThat(eventEntity.eventReference).isEqualTo("112414323")
      assertThat(eventEntity.location).isEqualTo("MDI")
      assertThat(eventEntity.modifiedBy).isEqualTo("user1")
    }

    @Test
    fun `missing report title adopts a fallback value`() {
      val reportDto = minimalReportDto.copy(title = null)

      val reportEntity = Event.createReport(reportDto)
      assertThat(reportEntity.title).isEqualTo("NO DETAILS GIVEN")
      assertThat(reportEntity.description).isEqualTo("DESCRIPTION")

      val eventEntity = reportEntity.event
      assertThat(eventEntity.title).isEqualTo("NO DETAILS GIVEN")
      assertThat(eventEntity.description).isEqualTo("DESCRIPTION")
    }

    @Test
    fun `missing report description adopts a fallback value`() {
      val reportDto = minimalReportDto.copy(description = null)

      val reportEntity = Event.createReport(reportDto)
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
    private val yesterday = now.minusDays(1)

    /** existing report created in NOMIS yesterday by a different user */
    private fun buildExistingReport() = buildReport(
      reportReference = "112414323",
      reportingUsername = "old-user",
      reportTime = yesterday,
      source = InformationSource.NOMIS,
    )

    @Test
    fun `report details should be copied to existing report and event`() {
      val existingReportEntity = buildExistingReport()
      val reportDto = minimalReportDto.copy()

      existingReportEntity.updateWith(
        upsert = reportDto,
        clock = clock,
      )
      assertThat(existingReportEntity.title).isEqualTo("TITLE")
      assertThat(existingReportEntity.description).isEqualTo("DESCRIPTION")
      assertThat(existingReportEntity.createdAt).isEqualTo(now.minusMinutes(4))
      assertThat(existingReportEntity.modifiedAt).isEqualTo(now.minusMinutes(4))
      assertThat(existingReportEntity.modifiedBy).isEqualTo("user1")
      assertThat(existingReportEntity.source).isEqualTo(InformationSource.NOMIS)
      assertThat(existingReportEntity.modifiedIn).isEqualTo(InformationSource.NOMIS)

      val eventEntity = existingReportEntity.event
      assertThat(eventEntity.title).isEqualTo("TITLE")
      assertThat(eventEntity.description).isEqualTo("DESCRIPTION")
      assertThat(eventEntity.createdAt).isEqualTo(now.minusMinutes(4))
      assertThat(eventEntity.modifiedAt).isEqualTo(now.minusMinutes(4))
      assertThat(eventEntity.modifiedBy).isEqualTo("user1")
    }

    @Test
    fun `missing report title adopts a fallback value`() {
      val existingReportEntity = buildExistingReport()
      val reportDto = minimalReportDto.copy(title = null)

      existingReportEntity.updateWith(
        upsert = reportDto,
        clock = clock,
      )
      assertThat(existingReportEntity.title).isEqualTo("NO DETAILS GIVEN")
      assertThat(existingReportEntity.description).isEqualTo("DESCRIPTION")

      val eventEntity = existingReportEntity.event
      assertThat(eventEntity.title).isEqualTo("NO DETAILS GIVEN")
      assertThat(eventEntity.description).isEqualTo("DESCRIPTION")
    }

    @Test
    fun `missing report description adopts a fallback value`() {
      val existingReportEntity = buildExistingReport()
      val reportDto = minimalReportDto.copy(description = null)

      existingReportEntity.updateWith(
        upsert = reportDto,
        clock = clock,
      )
      assertThat(existingReportEntity.title).isEqualTo("TITLE")
      assertThat(existingReportEntity.description).isEqualTo("NO DETAILS GIVEN")

      val eventEntity = existingReportEntity.event
      assertThat(eventEntity.title).isEqualTo("TITLE")
      assertThat(eventEntity.description).isEqualTo("NO DETAILS GIVEN")
    }

    @Test
    fun `changing status persists previous one in history`() {
      val existingReportEntity = buildExistingReport()
      val reportDto = minimalReportDto.copy()

      existingReportEntity.updateWith(
        upsert = reportDto,
        clock = clock,
      )
      assertThat(existingReportEntity.historyOfStatuses).hasSize(2)
      assertThat(existingReportEntity.historyOfStatuses.map { it.toDto() })
        .containsExactly(
          StatusHistory(Status.DRAFT, yesterday, "old-user"),
          StatusHistory(Status.AWAITING_ANALYSIS, now, "user1"),
        )

      val anotherReportDto = minimalReportDto.copy(description = "Status is unchanged")
      existingReportEntity.updateWith(
        upsert = anotherReportDto,
        clock = clock,
      )
      assertThat(existingReportEntity.historyOfStatuses).hasSize(2)
      assertThat(existingReportEntity.historyOfStatuses.map { it.toDto() })
        .containsExactly(
          StatusHistory(Status.DRAFT, yesterday, "old-user"),
          StatusHistory(Status.AWAITING_ANALYSIS, now, "user1"),
        )
    }
  }
}
