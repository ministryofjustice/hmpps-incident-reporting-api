package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.clock
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.now
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.util.UUID

class PrisonerMergeTest {
  private val reportRepository: ReportRepository = mock()
  private val eventRepository: EventRepository = mock()
  private val prisonerInvolvementRepository: PrisonerInvolvementRepository = mock()
  private val authenticationHolder: HmppsAuthenticationHolder = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val reportService = ReportService(
    reportRepository = reportRepository,
    eventRepository = eventRepository,
    prisonerInvolvementRepository = prisonerInvolvementRepository,
    telemetryClient = telemetryClient,
    authenticationHolder = authenticationHolder,
    clock = clock,
  )

  @Test
  fun `returns empty list when no rename takes place`() {
    whenever(prisonerInvolvementRepository.findAllByPrisonerNumber("A0002AA"))
      .thenReturn(emptyList())

    val reports = reportService.replacePrisonerNumber("A0002AA", "A0002BB")
    assertThat(reports).isEmpty()
  }

  @Test
  fun `can replace all instances of a prisoner number in prisoner involvement entities`() {
    // report with A0001AA
    val mockReport1 = buildReport(
      reportReference = "94728",
      reportTime = now.minusDays(3),
      generatePrisonerInvolvement = 1,
    ).also { it.id = UUID.fromString("066ab92f-efc5-7015-8000-42c0bc6b704b") }
    // report with A0001AA and A0002AA
    val mockReport2 = buildReport(
      reportReference = "IR-0000000001124143",
      reportTime = now.minusDays(2),
      generatePrisonerInvolvement = 2,
    ).also { it.id = UUID.fromString("066ab930-b9ef-7b6d-8000-23258e439e22") }
    // report with A0001AA, A0002AA and A0003AA
    val mockReport3 = buildReport(
      reportReference = "IR-0000000001124146",
      reportTime = now.minusDays(1),
      generatePrisonerInvolvement = 3,
    ).also { it.id = UUID.fromString("066ab931-77d5-70fc-8000-67fbea4733e5") }
    val mockReports = listOf(mockReport1, mockReport2, mockReport3)
    val mockPrisonerInvolvements = mockReports
      .flatMap { it.prisonersInvolved }
      .filter { it.prisonerNumber == "A0002AA" }

    whenever(prisonerInvolvementRepository.findAllByPrisonerNumber("A0002AA"))
      .thenReturn(mockPrisonerInvolvements)

    val reports = reportService.replacePrisonerNumber("A0002AA", "A0002BB")
    val reportIds = reports.map { it.reportReference }
    assertThat(reportIds)
      .isEqualTo(listOf("IR-0000000001124143", "IR-0000000001124146"))

    assertThat(mockReport1.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA"))
    assertThat(mockReport2.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA", "A0002BB"))
    assertThat(mockReport3.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA", "A0002BB", "A0003AA"))
    assertThat(mockReport1.modifiedAt).isBefore(now)
    assertThat(mockReport2.modifiedAt).isEqualTo(now)
    assertThat(mockReport3.modifiedAt).isEqualTo(now)
  }

  @Test
  fun `does not deduplicate situations where a prisoner number is involved more than once`() {
    // report with A0001AA and A0002AA
    val mockReport = buildReport(
      reportReference = "94728",
      reportTime = now,
      generatePrisonerInvolvement = 2,
    ).also { it.id = UUID.fromString("066ab92f-efc5-7015-8000-42c0bc6b704b") }
    val mockPrisonerInvolvements = mockReport.prisonersInvolved
      .filter { it.prisonerNumber == "A0002AA" }

    whenever(prisonerInvolvementRepository.findAllByPrisonerNumber("A0002AA"))
      .thenReturn(mockPrisonerInvolvements)

    val reports = reportService.replacePrisonerNumber("A0002AA", "A0001AA")
    val reportIds = reports.map { it.reportReference }
    assertThat(reportIds)
      .isEqualTo(listOf("94728"))

    assertThat(mockReport.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA", "A0001AA"))
  }
}
