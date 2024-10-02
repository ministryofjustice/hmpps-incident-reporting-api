package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.clock
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase.Companion.now
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.util.UUID

@DisplayName("Prisoner moving and merging")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
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

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Test
  fun `returns empty list when no rename takes place`() {
    whenever(prisonerInvolvementRepository.findAllByPrisonerNumber("A0002AA"))
      .thenReturn(emptyList())

    val reports = reportService.replacePrisonerNumber("A0002AA", "A0002BB")
    assertThat(reports).isEmpty()

    verify(telemetryClient, never())
      .trackEvent(any(), any(), anyOrNull())
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
      reportReference = "11124143",
      reportTime = now.minusDays(2),
      generatePrisonerInvolvement = 2,
    ).also { it.id = UUID.fromString("066ab930-b9ef-7b6d-8000-23258e439e22") }
    // report with A0001AA, A0002AA and A0003AA
    val mockReport3 = buildReport(
      reportReference = "11124146",
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
      .isEqualTo(listOf("11124143", "11124146"))

    assertThat(mockReport1.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA"))
    assertThat(mockReport2.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA", "A0002BB"))
    assertThat(mockReport3.prisonersInvolved.map { it.prisonerNumber })
      .isEqualTo(listOf("A0001AA", "A0002BB", "A0003AA"))
    assertThat(mockReport1.modifiedAt).isBefore(now)
    assertThat(mockReport2.modifiedAt).isEqualTo(now)
    assertThat(mockReport3.modifiedAt).isEqualTo(now)

    verify(telemetryClient, times(2))
      .trackEvent(eq("Prisoner A0002AA merged into A0002BB"), any(), isNull())
  }

  @Test
  fun `does not de-duplicate situations where a prisoner number is involved more than once`() {
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

  @Test
  fun `parses prisoner merge domain event`() {
    val mockReportService: ReportService = mock()
    val listener = PrisonOffenderEventListener(
      reportService = mockReportService,
      mapper = objectMapper,
    )
    listener.onPrisonOffenderEvent(
      // language=json
      """
      {
        "Type": "Notification",
        "MessageId": "5b90ee7d-67bc-5959-a4d8-b7d420180853",
        "Message": "{\"eventType\": \"prison-offender-events.prisoner.merged\", \"version\": \"1.0\", \"occurredAt\": \"2020-02-12T15:14:24.125533+00:00\", \"publishedAt\": \"2020-02-12T15:15:09.902048716+00:00\", \"description\": \"A prisoner has been merged from A0002AA to A0002BB\", \"additionalInformation\": {\"nomsNumber\": \"A0002BB\", \"removedNomsNumber\": \"A0002AA\", \"reason\": \"MERGE\"}}",
        "Timestamp": "2021-09-01T09:18:28.725Z",
        "MessageAttributes": {
          "eventType": {
            "Type": "String",
            "Value": "prison-offender-events.prisoner.merged"
          }
        }
      }
      """,
    )

    verify(mockReportService, times(1))
      .replacePrisonerNumber(eq("A0002AA"), eq("A0002BB"))
  }

  @Test
  fun `parses prisoner booking moved domain event`() {
    val mockReportService: ReportService = mock()
    val listener = PrisonOffenderEventListener(
      reportService = mockReportService,
      mapper = objectMapper,
    )
    listener.onPrisonOffenderEvent(
      // language=json
      """
      {
        "Type": "Notification",
        "MessageId": "5b90ee7d-67bc-5959-a4d8-b7d420180853",
        "Message": "{\"version\":\"1.0\",\"eventType\":\"prison-offender-events.prisoner.booking.moved\",\"occurredAt\":\"2020-02-12T15:14:24.125533+00:00\",\"publishedAt\":\"2020-02-12T15:15:09.902048716+00:00\",\"description\":\"a NOMIS booking has moved between prisoners\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"A0002BB\"}]},\"additionalInformation\":{\"bookingId\":\"123000\",\"movedToNomsNumber\":\"A0002BB\",\"movedFromNomsNumber\":\"A0002AA\"}}",
        "Timestamp": "2021-09-01T09:18:28.725Z",
        "MessageAttributes": {
          "eventType": {
            "Type": "String",
            "Value": "prison-offender-events.prisoner.booking.moved"
          }
        }
      }
      """,
    )

    verify(mockReportService, times(1))
      .replacePrisonerNumber(eq("A0002AA"), eq("A0002BB"))
  }
}
