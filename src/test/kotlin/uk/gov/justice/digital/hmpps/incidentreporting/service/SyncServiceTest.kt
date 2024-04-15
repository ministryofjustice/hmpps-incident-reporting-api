package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.clock
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentEvent
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.CodeDescription
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Offender
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.OffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Staff
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.StaffParty
import uk.gov.justice.digital.hmpps.incidentreporting.resource.IncidentReportNotFoundException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.UpsertNomisIncident
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport as IncidentReportDto

class SyncServiceTest {
  private val reportRepository: IncidentReportRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val syncService = SyncService(
    reportRepository,
    clock,
    telemetryClient,
  )

  private val now = LocalDateTime.now(clock)
  private val whenIncidentHappened = now.minusDays(1)

  private val reportedBy = "user2"

  /** incident report upsert request – copied with modifications in tests */
  private val sampleUpsert = UpsertNomisIncident(
    incidentReport = NomisIncidentReport(
      incidentId = 112414323,
      questionnaireId = 2124,
      title = "Cutting",
      description = "Offender was found in own cell with a razor",
      prison = CodeDescription("MDI", "Moorland (HMP)"),
      status = NomisIncidentStatus("AWAN", "Awaiting Analysis"),
      type = "SELF_HARM",
      lockedResponse = false,
      incidentDateTime = whenIncidentHappened,
      reportingStaff = Staff(reportedBy, 121, "John", "Smith"),
      reportedDateTime = now,
      staffParties = listOf(
        StaffParty(
          Staff("user3", 124, "Mary", "Jones"),
          CodeDescription("PAS", "Present at scene"),
          "Found offender in cell",
        ),
      ),
      offenderParties = listOf(
        OffenderParty(
          offender = Offender(
            offenderNo = "A1234AA",
            firstName = "Trevor",
            lastName = "Smith",
          ),
          role = CodeDescription("PERP", "Perpetrator"),
          outcome = CodeDescription("HELTH", "ACCT"),
          comment = "First time self-harming",
        ),
      ),
      requirements = emptyList(),
      questions = emptyList(),
      history = emptyList(),
    ),
  )

  private val sampleReportId = UUID.fromString("11111111-2222-3333-4444-555555555555")

  /** saved entity based on successful `sampleUpsert` */
  private val sampleReport = IncidentReport(
    id = sampleReportId,
    incidentNumber = "112414323",
    incidentDateAndTime = whenIncidentHappened,
    incidentType = IncidentType.SELF_HARM,
    summary = "Cutting",
    incidentDetails = "Offender was found in own cell with a razor",
    prisonId = "MDI",
    event = IncidentEvent(
      eventId = "112414323",
      eventDateAndTime = whenIncidentHappened,
      prisonId = "MDI",
      eventDetails = "Offender was found in own cell with a razor",
      createdDate = now,
      lastModifiedDate = now,
      lastModifiedBy = reportedBy,
    ),
    reportedBy = reportedBy,
    reportedDate = now,
    status = IncidentStatus.AWAITING_ANALYSIS,
    assignedTo = reportedBy,
    source = InformationSource.NOMIS,
    createdDate = now,
    lastModifiedDate = now,
    lastModifiedBy = reportedBy,
  )

  /** compare report entity about to be saved with the mocked response */
  private fun isEqualToSampleReport(report: IncidentReport, expectedId: UUID?): Boolean {
    // NB: cannot compare arg to sampleReport because IncidentReport.equals only compares incidentNumber
    return report.id == expectedId &&
      report.incidentNumber == sampleReport.incidentNumber &&
      report.incidentDateAndTime == sampleReport.incidentDateAndTime &&
      report.getIncidentType() == sampleReport.getIncidentType() &&
      report.summary == sampleReport.summary &&
      report.incidentDetails == sampleReport.incidentDetails &&
      report.prisonId == sampleReport.prisonId &&
      report.reportedBy == sampleReport.reportedBy &&
      report.reportedDate == sampleReport.reportedDate &&
      report.status == sampleReport.status &&
      report.assignedTo == sampleReport.assignedTo &&
      report.source == sampleReport.source &&
      report.createdDate == sampleReport.createdDate &&
      report.lastModifiedDate == sampleReport.lastModifiedDate &&
      report.lastModifiedBy == sampleReport.lastModifiedBy &&
      isEqualToSampleEvent(report.event)
  }

  /** compare event entity about to be saved with the mocked response */
  private fun isEqualToSampleEvent(event: IncidentEvent): Boolean {
    // NB: cannot compare arg to sampleReport because IncidentEvent.equals only compares eventId
    val sampleEvent = sampleReport.event
    return event.eventId == sampleEvent.eventId &&
      event.eventDateAndTime == sampleEvent.eventDateAndTime &&
      event.prisonId == sampleEvent.prisonId &&
      event.eventDetails == sampleEvent.eventDetails &&
      event.createdDate == sampleEvent.createdDate &&
      event.lastModifiedDate == sampleEvent.lastModifiedDate &&
      event.lastModifiedBy == sampleEvent.lastModifiedBy
  }

  private fun assertSampleReportConvertedToDto(report: IncidentReportDto) {
    assertThat(report.id).isEqualTo(sampleReportId)
    assertThat(report.incidentNumber).isEqualTo("112414323")
    assertThat(report.incidentType).isEqualTo(IncidentType.SELF_HARM)
    assertThat(report.incidentDateAndTime).isEqualTo(whenIncidentHappened)
    assertThat(report.prisonId).isEqualTo("MDI")
    assertThat(report.summary).isEqualTo("Cutting")
    assertThat(report.incidentDetails).isEqualTo("Offender was found in own cell with a razor")
    assertThat(report.event.eventId).isEqualTo("112414323")
    assertThat(report.event.eventDateAndTime).isEqualTo(whenIncidentHappened)
    assertThat(report.event.eventDetails).isEqualTo("Offender was found in own cell with a razor")
    assertThat(report.event.prisonId).isEqualTo("MDI")
    assertThat(report.reportedBy).isEqualTo(reportedBy)
    assertThat(report.reportedDate).isEqualTo(now)
    assertThat(report.status).isEqualTo(IncidentStatus.AWAITING_ANALYSIS)
    assertThat(report.assignedTo).isEqualTo(reportedBy)
    assertThat(report.createdDate).isEqualTo(now)
    assertThat(report.lastModifiedDate).isEqualTo(now)
    assertThat(report.lastModifiedBy).isEqualTo(reportedBy)
    assertThat(report.createdInNomis).isTrue()

    // TODO: compare more DTO properties once implemented in IR-196:
    //   • staff & prisoner involvement
    //   • questions & responses
    //   • evidence
    //   • corrections
    //   • history
  }

  @ParameterizedTest(name = "can sync a new report during initial migration: {0}")
  @ValueSource(booleans = [true, false])
  fun `can sync a new report during initial migration`(initialMigration: Boolean) {
    val upsert = sampleUpsert.copy(
      id = null,
      initialMigration = initialMigration,
    )

    whenever(reportRepository.save(any())).thenReturn(sampleReport)

    val report = syncService.upsert(upsert)

    // verify that correct entity was to be saved
    verify(reportRepository).save(
      argThat { it -> isEqualToSampleReport(it, null) },
    )

    // verify sampleReport is correctly converted into DTO
    assertSampleReportConvertedToDto(report)

    // verify telemetry is sent
    verify(telemetryClient).trackEvent(
      "Synchronised Incident Report",
      mapOf(
        "created" to "true",
        "updated" to "false",
        "id" to "11111111-2222-3333-4444-555555555555",
        "prisonId" to "MDI",
      ),
      null,
    )
  }

  @Test
  fun `can update existing report`() {
    val upsert = sampleUpsert.copy(
      id = sampleReportId,
      initialMigration = false,
    )

    whenever(reportRepository.findById(sampleReportId)).thenReturn(Optional.of(sampleReport))

    val report = syncService.upsert(upsert)

    // TODO: CANNOT verify that correct entity was to be saved,
    //   `reportRepository.save` not explicitly called

    // verify sampleReport is correctly converted into DTO
    assertSampleReportConvertedToDto(report)

    // verify telemetry is sent
    verify(telemetryClient).trackEvent(
      "Synchronised Incident Report",
      mapOf(
        "created" to "false",
        "updated" to "true",
        "id" to "11111111-2222-3333-4444-555555555555",
        "prisonId" to "MDI",
      ),
      null,
    )
  }

  @Test
  fun `cannot update missing report`() {
    val missingId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val upsert = sampleUpsert.copy(
      id = missingId,
      initialMigration = false,
    )

    whenever(reportRepository.findById(missingId)).thenReturn(Optional.empty())

    assertThatThrownBy {
      syncService.upsert(upsert)
    }.isInstanceOf(IncidentReportNotFoundException::class.java)

    // verify entity not saved
    verify(reportRepository, never()).save(any())

    // verify telemetry not sent
    verify(telemetryClient, never()).trackEvent(anyOrNull(), anyOrNull(), anyOrNull())
  }
}