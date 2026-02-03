package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.incidentreporting.dto.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.PrisonerInvolvementRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepositoryCustom
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("Prisoner merging and booking moving")
class PrisonerMergeTest : SqsIntegrationTestBase() {
  private val reportRepository: ReportRepository = mock()
  private val reportRepositoryCustom: ReportRepositoryCustom = mock()
  private val prisonerInvolvementRepository: PrisonerInvolvementRepository = mock()
  private val prisonerSearchService: PrisonerSearchService = mock()
  private val correctionRequestService: CorrectionRequestService = mock()
  private val authenticationHolder: HmppsAuthenticationHolder = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val reportService = ReportService(
    reportRepository = reportRepository,
    reportRepositoryCustom = reportRepositoryCustom,
    prisonerInvolvementRepository = prisonerInvolvementRepository,
    prisonerSearchService = prisonerSearchService,
    telemetryClient = telemetryClient,
    authenticationHolder = authenticationHolder,
    correctionRequestService = correctionRequestService,
    clock = clock,
  )

  @DisplayName("Replacing prisoner numbers")
  @Nested
  inner class ReplacePrisonerNumbers {
    // report involving A0001AA from 3 days ago
    private lateinit var mockReport1: Report

    // report involving A0001AA and A0002AA from 2 days ago
    private lateinit var mockReport2: Report

    // report involving A0001AA, A0002AA and A0003AA from yesterday
    private lateinit var mockReport3: Report

    @BeforeEach
    fun setUp() {
      // setup 3 reports as described above
      mockReport1 = buildReport(
        reportReference = "94728",
        reportTime = now.minusDays(3),
        generatePrisonerInvolvement = 1,
      ).also { it.id = UUID.fromString("066ab92f-efc5-7015-8000-42c0bc6b704b") }
      mockReport2 = buildReport(
        reportReference = "11124143",
        reportTime = now.minusDays(2),
        generatePrisonerInvolvement = 2,
      ).also { it.id = UUID.fromString("066ab930-b9ef-7b6d-8000-23258e439e22") }
      mockReport3 = buildReport(
        reportReference = "11124146",
        reportTime = now.minusDays(1),
        generatePrisonerInvolvement = 3,
      ).also { it.id = UUID.fromString("066ab931-77d5-70fc-8000-67fbea4733e5") }
      val allMockReports = listOf(mockReport1, mockReport2, mockReport3)

      // mock repository method to return filtered prisoner involvements built above
      whenever(prisonerInvolvementRepository.findAllByPrisonerNumber(any()))
        .then { mock ->
          val prisonerNumber = mock.arguments[0] as String
          allMockReports
            .flatMap { it.prisonersInvolved }
            .filter { it.prisonerNumber == prisonerNumber }
        }
    }

    @Test
    fun `returns empty list when no rename takes place`() {
      whenever(prisonerInvolvementRepository.findAllByPrisonerNumber("A0002AA"))
        .thenReturn(emptyList())

      val reports = reportService.replacePrisonerNumber("A0002AA", "A0002BB")
      assertThat(reports).isEmpty()

      val reportsInDateRange = reportService.replacePrisonerNumberInDateRange(
        "A0002AA",
        "A0002BB",
        LocalDateTime.parse("2023-11-17T12:34:56.123456"),
        now,
      )
      assertThat(reportsInDateRange).isEmpty()

      verify(prisonerSearchService, never())
        .searchByPrisonerNumbers(any())

      verify(telemetryClient, never())
        .trackEvent(any(), any(), anyOrNull())
    }

    @Test
    fun `can replace all instances of a prisoner number in prisoner involvement entities`() {
      whenever(prisonerSearchService.searchByPrisonerNumbers(setOf("A0002BB")))
        .thenReturn(
          mapOf(
            "A0002BB" to Prisoner(
              prisonerNumber = "A0002BB",
              firstName = "New first name",
              lastName = "New surname",
            ),
          ),
        )

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
      assertThat(mockReport1.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1"))
      assertThat(mockReport2.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1", "New surname, New first name"))
      assertThat(mockReport3.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1", "New surname, New first name", "Last 3, First 3"))
      assertThat(mockReport1.modifiedAt).isBefore(now)
      assertThat(mockReport2.modifiedAt).isEqualTo(now)
      assertThat(mockReport3.modifiedAt).isEqualTo(now)

      verify(prisonerSearchService, times(1))
        .searchByPrisonerNumbers(setOf("A0002BB"))

      verify(telemetryClient, times(2))
        .trackEvent(
          eq("Prisoner A0002AA replaced with A0002BB (reported between whenever and now)"),
          argThat {
            listOf("11124143", "11124146").contains(get("reportReference"))
          },
          isNull(),
        )
    }

    @Test
    fun `can replace instances of a prisoner number in prisoner involvement entities for a date range`() {
      whenever(prisonerSearchService.searchByPrisonerNumbers(setOf("A0001BB")))
        .thenReturn(
          mapOf(
            "A0001BB" to Prisoner(
              prisonerNumber = "A0001BB",
              firstName = "New first name",
              lastName = "New surname",
            ),
          ),
        )

      val reports = reportService.replacePrisonerNumberInDateRange(
        "A0001AA",
        "A0001BB",
        now.minusDays(2),
        now.minusDays(2),
      )
      val reportIds = reports.map { it.reportReference }
      assertThat(reportIds)
        .isEqualTo(listOf("11124143"))

      assertThat(mockReport1.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001AA"))
      assertThat(mockReport2.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001BB", "A0002AA"))
      assertThat(mockReport3.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001AA", "A0002AA", "A0003AA"))
      assertThat(mockReport1.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1"))
      assertThat(mockReport2.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("New surname, New first name", "Last 2, First 2"))
      assertThat(mockReport3.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1", "Last 2, First 2", "Last 3, First 3"))
      assertThat(mockReport1.modifiedAt).isBefore(now)
      assertThat(mockReport2.modifiedAt).isEqualTo(now)
      assertThat(mockReport3.modifiedAt).isBefore(now)

      verify(prisonerSearchService, times(1))
        .searchByPrisonerNumbers(setOf("A0001BB"))

      verify(telemetryClient, times(1))
        .trackEvent(
          eq("Prisoner A0001AA replaced with A0001BB (reported between ${now.minusDays(2)} and ${now.minusDays(2)})"),
          argThat {
            listOf("11124143").contains(get("reportReference"))
          },
          isNull(),
        )
    }

    @Test
    fun `can replace instances of a prisoner number in prisoner involvement entities since a date`() {
      whenever(prisonerSearchService.searchByPrisonerNumbers(setOf("A0001BB")))
        .thenReturn(
          mapOf(
            "A0001BB" to Prisoner(
              prisonerNumber = "A0001BB",
              firstName = "New first name",
              lastName = "New surname",
            ),
          ),
        )

      val reports = reportService.replacePrisonerNumberInDateRange("A0001AA", "A0001BB", now.minusDays(2), null)
      val reportIds = reports.map { it.reportReference }
      assertThat(reportIds)
        .isEqualTo(listOf("11124143", "11124146"))

      assertThat(mockReport1.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001AA"))
      assertThat(mockReport2.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001BB", "A0002AA"))
      assertThat(mockReport3.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001BB", "A0002AA", "A0003AA"))
      assertThat(mockReport1.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1"))
      assertThat(mockReport2.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("New surname, New first name", "Last 2, First 2"))
      assertThat(mockReport3.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("New surname, New first name", "Last 2, First 2", "Last 3, First 3"))
      assertThat(mockReport1.modifiedAt).isBefore(now)
      assertThat(mockReport2.modifiedAt).isEqualTo(now)
      assertThat(mockReport3.modifiedAt).isEqualTo(now)

      verify(prisonerSearchService, times(1))
        .searchByPrisonerNumbers(setOf("A0001BB"))

      verify(telemetryClient, times(2))
        .trackEvent(
          eq("Prisoner A0001AA replaced with A0001BB (reported between ${now.minusDays(2)} and now)"),
          argThat {
            listOf("11124143", "11124146").contains(get("reportReference"))
          },
          isNull(),
        )
    }

    @Test
    fun `can replace instances of a prisoner number in prisoner involvement entities until a date`() {
      whenever(prisonerSearchService.searchByPrisonerNumbers(setOf("A0001BB")))
        .thenReturn(
          mapOf(
            "A0001BB" to Prisoner(
              prisonerNumber = "A0001BB",
              firstName = "New first name",
              lastName = "New surname",
            ),
          ),
        )

      val reports = reportService.replacePrisonerNumberInDateRange("A0001AA", "A0001BB", null, now.minusDays(2))
      val reportIds = reports.map { it.reportReference }
      assertThat(reportIds)
        .isEqualTo(listOf("94728", "11124143"))

      assertThat(mockReport1.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001BB"))
      assertThat(mockReport2.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001BB", "A0002AA"))
      assertThat(mockReport3.prisonersInvolved.map { it.prisonerNumber })
        .isEqualTo(listOf("A0001AA", "A0002AA", "A0003AA"))
      assertThat(mockReport1.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("New surname, New first name"))
      assertThat(mockReport2.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("New surname, New first name", "Last 2, First 2"))
      assertThat(mockReport3.prisonersInvolved.map { "${it.lastName}, ${it.firstName}" })
        .isEqualTo(listOf("Last 1, First 1", "Last 2, First 2", "Last 3, First 3"))
      assertThat(mockReport1.modifiedAt).isEqualTo(now)
      assertThat(mockReport2.modifiedAt).isEqualTo(now)
      assertThat(mockReport3.modifiedAt).isBefore(now)

      verify(prisonerSearchService, times(1))
        .searchByPrisonerNumbers(setOf("A0001BB"))

      verify(telemetryClient, times(2))
        .trackEvent(
          eq("Prisoner A0001AA replaced with A0001BB (reported between whenever and ${now.minusDays(2)})"),
          argThat {
            listOf("94728", "11124143").contains(get("reportReference"))
          },
          isNull(),
        )
    }
  }

  @Test
  fun `does not de-duplicate situations where a prisoner number is involved more than once`() {
    whenever(prisonerSearchService.searchByPrisonerNumbers(setOf("A0001AA")))
      .thenReturn(
        mapOf(
          "A0001AA" to Prisoner(
            prisonerNumber = "A0001AA",
            firstName = "New first name",
            lastName = "New surname",
          ),
        ),
      )

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
      zoneId = zoneId,
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
  fun `parses (old) prisoner booking moved domain event that does not specify booking start date`() {
    val mockReportService: ReportService = mock()
    val listener = PrisonOffenderEventListener(
      reportService = mockReportService,
      mapper = objectMapper,
      zoneId = zoneId,
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

    verify(mockReportService, never())
      .replacePrisonerNumber(eq("A0002AA"), eq("A0002BB"))
    verify(mockReportService, times(1))
      .replacePrisonerNumberInDateRange(
        eq("A0002AA"),
        eq("A0002BB"),
        isNull(),
        isNull(),
      )
  }

  @Test
  fun `parses (new) prisoner booking moved domain event that specifies booking start date`() {
    val mockReportService: ReportService = mock()
    val listener = PrisonOffenderEventListener(
      reportService = mockReportService,
      mapper = objectMapper,
      zoneId = zoneId,
    )
    listener.onPrisonOffenderEvent(
      // language=json
      """
      {
        "Type": "Notification",
        "MessageId": "5b90ee7d-67bc-5959-a4d8-b7d420180853",
        "Message": "{\"version\":\"1.0\",\"eventType\":\"prison-offender-events.prisoner.booking.moved\",\"occurredAt\":\"2020-02-12T15:14:24.125533+00:00\",\"publishedAt\":\"2020-02-12T15:15:09.902048716+00:00\",\"description\":\"a NOMIS booking has moved between prisoners\",\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"A0002BB\"}]},\"additionalInformation\":{\"bookingId\":\"123000\",\"movedToNomsNumber\":\"A0002BB\",\"movedFromNomsNumber\":\"A0002AA\",\"bookingStartDateTime\":\"2019-12-13T06:07:08\"}}",
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

    verify(mockReportService, never())
      .replacePrisonerNumber(eq("A0002AA"), eq("A0002BB"))
    verify(mockReportService, times(1))
      .replacePrisonerNumberInDateRange(
        eq("A0002AA"),
        eq("A0002BB"),
        eq(LocalDateTime.parse("2019-12-13T06:07:08")),
        eq(LocalDateTime.parse("2020-02-12T15:14:24.125533")),
      )
  }
}
