package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.DisplayName
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
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisCode
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisOffender
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisOffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisRequirement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisResponse
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaff
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaffParty
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStatus
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.clock
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.now
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportAlreadyExistsException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.sql.SQLException
import java.util.UUID

@DisplayName("NOMIS sync service")
class NomisSyncServiceTest {
  private val reportRepository: ReportRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val syncService = NomisSyncService(
    reportRepository,
    clock,
    telemetryClient,
  )

  private val whenIncidentHappened = now.minusDays(1)

  private val reportedBy = "user2"

  /** incident report sync request – copied with modifications in tests */
  private val sampleSyncRequest = NomisSyncRequest(
    incidentReport = NomisReport(
      incidentId = 112414323,
      questionnaireId = 2124,
      title = "Cutting",
      description = "Offender was found in own cell with a razor",
      prison = NomisCode("MDI", "Moorland (HMP)"),
      status = NomisStatus("AWAN", "Awaiting Analysis"),
      type = "SELF_HARM",
      lockedResponse = false,
      incidentDateTime = whenIncidentHappened,
      reportingStaff = NomisStaff(reportedBy, 121, "John", "Smith"),
      reportedDateTime = now,
      createDateTime = now.plusHours(2),
      createdBy = reportedBy,
      lastModifiedDateTime = now.plusHours(5),
      lastModifiedBy = "another-user",
      staffParties = listOf(
        NomisStaffParty(
          NomisStaff("user3", 124, "Mary", "Jones"),
          NomisCode("PAS", "Present at scene"),
          "Found offender in cell",
          createDateTime = now,
          createdBy = reportedBy,
        ),
      ),
      offenderParties = listOf(
        NomisOffenderParty(
          offender = NomisOffender(
            offenderNo = "A1234AA",
            firstName = "Trevor",
            lastName = "Smith",
          ),
          role = NomisCode("PERP", "Perpetrator"),
          outcome = NomisCode("HELTH", "ACCT"),
          comment = "First time self-harming",
          createDateTime = now,
          createdBy = reportedBy,
        ),
      ),
      requirements = listOf(
        NomisRequirement(
          comment = "Title should include prisoner number",
          date = now.toLocalDate(),
          prisonId = "MDI",
          staff = NomisStaff(
            staffId = 42,
            username = "checking-user",
            firstName = "John",
            lastName = "McCheckin-User",
          ),
          createDateTime = now,
          createdBy = reportedBy,
        ),
      ),
      questions = listOf(
        NomisQuestion(
          questionId = 42,
          sequence = 1,
          createDateTime = now,
          createdBy = reportedBy,
          question = "What implement was used?",
          answers = listOf(
            NomisResponse(
              answer = "Razor",
              questionResponseId = null,
              sequence = 1,
              comment = null,
              createDateTime = now,
              createdBy = reportedBy,
              recordingStaff = NomisStaff(
                username = reportedBy,
                staffId = 42,
                firstName = "John",
                lastName = "Doe",
              ),
            ),
          ),
        ),
      ),
      history = emptyList(),
    ),
  )

  private val sampleReportId = UUID.fromString("11111111-2222-3333-4444-555555555555")

  /** saved entity based on successful `sampleSyncRequest` */
  private val sampleReport = Report(
    id = sampleReportId,
    incidentNumber = "112414323",
    incidentDateAndTime = whenIncidentHappened,
    type = Type.SELF_HARM,
    title = "Cutting",
    description = "Offender was found in own cell with a razor",
    prisonId = "MDI",
    event = Event(
      eventId = "112414323",
      eventDateAndTime = whenIncidentHappened,
      prisonId = "MDI",
      title = "Cutting",
      description = "Offender was found in own cell with a razor",
      createdAt = now.plusHours(2),
      modifiedAt = now.plusHours(5),
      modifiedBy = "another-user",
    ),
    reportedBy = reportedBy,
    reportedAt = now,
    status = Status.AWAITING_ANALYSIS,
    assignedTo = reportedBy,
    source = InformationSource.NOMIS,
    createdAt = now.plusHours(2),
    modifiedAt = now.plusHours(5),
    modifiedBy = "another-user",
  )

  init {
    sampleReport.addQuestion("QID-000000000042", "What implement was used?")
      .addResponse("Razor", null, reportedBy, now)
    sampleReport.addLocation("MDI-1-029", "CELL", "Wing 1, cell 029")
    sampleReport.addStaffInvolved(StaffRole.PRESENT_AT_SCENE, "user3", "Found offender in cell")
    sampleReport.addPrisonerInvolved(
      "A1234AA",
      PrisonerRole.PERPETRATOR,
      PrisonerOutcome.SEEN_HEALTHCARE,
      "First time self-harming",
    )
    sampleReport.addEvidence("CAM", "Body worn camera")
    sampleReport.addCorrectionRequest(
      "checking-user",
      now,
      CorrectionReason.NOT_SPECIFIED,
      "Title should include prisoner number",
    )
  }

  /** compare report entity about to be saved with the mocked response */
  private fun isEqualToSampleReport(report: Report, expectedId: UUID?): Boolean {
    // NB: cannot compare arg to sampleReport because IncidentReport.equals only compares incidentNumber
    return report.id == expectedId &&
      report.incidentNumber == sampleReport.incidentNumber &&
      report.incidentDateAndTime == sampleReport.incidentDateAndTime &&
      report.type == sampleReport.type &&
      report.title == sampleReport.title &&
      report.description == sampleReport.description &&
      report.prisonId == sampleReport.prisonId &&
      report.reportedBy == sampleReport.reportedBy &&
      report.reportedAt == sampleReport.reportedAt &&
      report.status == sampleReport.status &&
      report.assignedTo == sampleReport.assignedTo &&
      report.source == sampleReport.source &&
      report.createdAt == sampleReport.createdAt &&
      report.modifiedAt == sampleReport.modifiedAt &&
      report.modifiedBy == sampleReport.modifiedBy &&
      isEqualToSampleEvent(report.event)
  }

  /** compare event entity about to be saved with the mocked response */
  private fun isEqualToSampleEvent(event: Event): Boolean {
    // NB: cannot compare arg to sampleReport because IncidentEvent.equals only compares eventId
    val sampleEvent = sampleReport.event
    return event.eventId == sampleEvent.eventId &&
      event.eventDateAndTime == sampleEvent.eventDateAndTime &&
      event.prisonId == sampleEvent.prisonId &&
      event.description == sampleEvent.description &&
      event.createdAt == sampleEvent.createdAt &&
      event.modifiedAt == sampleEvent.modifiedAt &&
      event.modifiedBy == sampleEvent.modifiedBy
  }

  private fun assertSampleReportConvertedToDto(report: ReportWithDetails) {
    assertThat(report.id).isEqualTo(sampleReportId)
    assertThat(report.incidentNumber).isEqualTo("112414323")
    assertThat(report.type).isEqualTo(Type.SELF_HARM)
    assertThat(report.incidentDateAndTime).isEqualTo(whenIncidentHappened)
    assertThat(report.prisonId).isEqualTo("MDI")
    assertThat(report.title).isEqualTo("Cutting")
    assertThat(report.description).isEqualTo("Offender was found in own cell with a razor")
    assertThat(report.event.eventId).isEqualTo("112414323")
    assertThat(report.event.eventDateAndTime).isEqualTo(whenIncidentHappened)
    assertThat(report.event.description).isEqualTo("Offender was found in own cell with a razor")
    assertThat(report.event.prisonId).isEqualTo("MDI")
    assertThat(report.event.createdAt.toString()).isEqualTo("2023-12-05T14:34:56")
    assertThat(report.event.modifiedBy).isEqualTo("another-user")
    assertThat(report.event.modifiedAt.toString()).isEqualTo("2023-12-05T17:34:56")
    assertThat(report.reportedBy).isEqualTo(reportedBy)
    assertThat(report.reportedAt).isEqualTo(now)
    assertThat(report.status).isEqualTo(Status.AWAITING_ANALYSIS)
    assertThat(report.assignedTo).isEqualTo(reportedBy)
    assertThat(report.createdAt).isEqualTo(now.plusHours(2))
    assertThat(report.modifiedAt).isEqualTo(now.plusHours(5))
    assertThat(report.modifiedBy).isEqualTo("another-user")
    assertThat(report.createdInNomis).isTrue()

    assertThat(report.history).isEmpty()
    assertThat(report.historyOfStatuses).isEmpty()

    assertThat(report.questions).hasSize(1)
    val question = report.questions[0]
    assertThat(question.code).isEqualTo("QID-000000000042")
    assertThat(question.question).isEqualTo("What implement was used?")
    assertThat(question.additionalInformation).isNull()
    assertThat(question.responses).hasSize(1)
    val response = question.responses[0]
    assertThat(response.response).isEqualTo("Razor")
    assertThat(response.recordedBy).isEqualTo(reportedBy)
    assertThat(response.recordedAt).isEqualTo(now)
    assertThat(response.additionalInformation).isNull()

    assertThat(report.locations).hasSize(1)
    val location = report.locations[0]
    assertThat(location.locationId).isEqualTo("MDI-1-029")
    assertThat(location.type).isEqualTo("CELL")
    assertThat(location.description).isEqualTo("Wing 1, cell 029")

    assertThat(report.prisonersInvolved).hasSize(1)
    val prisonerInvolved = report.prisonersInvolved[0]
    assertThat(prisonerInvolved.prisonerNumber).isEqualTo("A1234AA")
    assertThat(prisonerInvolved.prisonerRole).isEqualTo(PrisonerRole.PERPETRATOR)
    assertThat(prisonerInvolved.outcome).isEqualTo(PrisonerOutcome.SEEN_HEALTHCARE)
    assertThat(prisonerInvolved.comment).isEqualTo("First time self-harming")

    assertThat(report.staffInvolved).hasSize(1)
    val staffInvolved = report.staffInvolved[0]
    assertThat(staffInvolved.staffUsername).isEqualTo("user3")
    assertThat(staffInvolved.staffRole).isEqualTo(StaffRole.PRESENT_AT_SCENE)
    assertThat(staffInvolved.comment).isEqualTo("Found offender in cell")

    assertThat(report.evidence).hasSize(1)
    val evidence = report.evidence[0]
    assertThat(evidence.type).isEqualTo("CAM")
    assertThat(evidence.description).isEqualTo("Body worn camera")

    assertThat(report.correctionRequests).hasSize(1)
    val correctionRequest = report.correctionRequests[0]
    assertThat(correctionRequest.correctionRequestedBy).isEqualTo("checking-user")
    assertThat(correctionRequest.correctionRequestedAt.toLocalDate()).isEqualTo(now.toLocalDate())
    assertThat(correctionRequest.reason).isEqualTo(CorrectionReason.NOT_SPECIFIED)
    assertThat(correctionRequest.descriptionOfChange).isEqualTo("Title should include prisoner number")
  }

  @ParameterizedTest(name = "can sync a new report when initial migration = {0}")
  @ValueSource(booleans = [true, false])
  fun `can sync a new report during initial migration`(initialMigration: Boolean) {
    val syncRequest = sampleSyncRequest.copy(
      id = null,
      initialMigration = initialMigration,
    )

    whenever(reportRepository.save(any())).thenReturn(sampleReport)

    val report = syncService.upsert(syncRequest)

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
        "incidentNumber" to "112414323",
        "prisonId" to "MDI",
      ),
      null,
    )
  }

  @Test
  fun `can update existing report`() {
    val syncRequest = sampleSyncRequest.copy(
      id = sampleReportId,
      initialMigration = false,
    )

    whenever(reportRepository.findOneEagerlyById(sampleReportId)).thenReturn(sampleReport)

    val report = syncService.upsert(syncRequest)

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
        "incidentNumber" to "112414323",
        "prisonId" to "MDI",
      ),
      null,
    )
  }

  @Test
  fun `cannot update missing report`() {
    val missingId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val syncRequest = sampleSyncRequest.copy(
      id = missingId,
      initialMigration = false,
    )

    whenever(reportRepository.findOneEagerlyById(missingId)).thenReturn(null)

    assertThatThrownBy {
      syncService.upsert(syncRequest)
    }.isInstanceOf(ReportNotFoundException::class.java)

    // TODO: CANNOT verify that no entity was saved,
    //   `reportRepository.save` not explicitly called

    // verify entity not saved
    verify(reportRepository, never()).save(any())

    // verify telemetry not sent
    verify(telemetryClient, never()).trackEvent(anyOrNull(), anyOrNull(), anyOrNull())
  }

  @Test
  fun `cannot update report during initial migration`() {
    val syncRequest = sampleSyncRequest.copy(
      id = sampleReportId,
      initialMigration = true,
    )

    assertThatThrownBy {
      syncService.upsert(syncRequest)
    }.isInstanceOf(ValidationException::class.java)

    // verify entity not even looked up
    verify(reportRepository, never()).findOneEagerlyById(any())

    // verify telemetry not sent
    verify(telemetryClient, never()).trackEvent(anyOrNull(), anyOrNull(), anyOrNull())
  }

  @ParameterizedTest(name = "throws report-already-exists exception if {0} constraint failed")
  @ValueSource(strings = ["event_id", "incident_number"])
  fun `throws report-already-exists exception if identifier constraints failed`(constraint: String) {
    val syncRequest = sampleSyncRequest.copy(
      id = null,
      initialMigration = true,
    )

    val abbreviatedMessage = "ERROR: duplicate key value violates unique constraint \"$constraint\""
    val simulatedDatabaseError = DataIntegrityViolationException(
      abbreviatedMessage,
      ConstraintViolationException(
        abbreviatedMessage,
        // in practice, a org.postgresql.util.PSQLException
        SQLException(abbreviatedMessage),
        constraint,
      ),
    )
    whenever(reportRepository.save(any())).thenThrow(simulatedDatabaseError)

    assertThatThrownBy {
      syncService.upsert(syncRequest)
    }.isInstanceOf(ReportAlreadyExistsException::class.java)
  }

  @Test
  fun `rethrows exception if caused by something other than identifier constraints`() {
    val syncRequest = sampleSyncRequest.copy(
      id = null,
      initialMigration = true,
    )

    val simulatedDatabaseError = DataIntegrityViolationException("unknown data integrity violation")
    whenever(reportRepository.save(any())).thenThrow(simulatedDatabaseError)

    assertThatThrownBy {
      syncService.upsert(syncRequest)
    }
      .isInstanceOf(DataIntegrityViolationException::class.java)
      .hasMessage("unknown data integrity violation")
  }
}
