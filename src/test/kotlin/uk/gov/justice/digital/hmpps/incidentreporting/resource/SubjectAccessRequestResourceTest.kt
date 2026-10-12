package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.LocalDateTime

@DisplayName("SAR resource")
class SubjectAccessRequestResourceTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var reportRepository: ReportRepository

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()

    reportRepository.save(
      buildReport(
        reportReference = "1000001",
        reportTime = now.minusMinutes(10),
        generatePrisonerInvolvement = 1,
      ),
    )
    reportRepository.save(
      buildReport(
        reportReference = "1000002",
        reportTime = now,
        generatePrisonerInvolvement = 2,
      ),
    )
  }

  @DisplayName("SAR endpoint is protected")
  @TestFactory
  fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
    webTestClient.get().uri("/subject-access-request?prn=A0003BB"),
    "SAR_DATA_ACCESS",
  )

  @Test
  fun `SAR endpoint returns reports if given prisoner is involved`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=A0001AA")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().json(
        // language=json
        """
        {
          "content": [
            {"reportReference":  "1000001"},
            {"reportReference":  "1000002"}
          ]
        }
        """,
        JsonCompareMode.LENIENT,
      )
  }

  @Test
  fun `SAR endpoint returns filtered report if given prisoner is involved`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=A0002AA")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().json(
        // language=json
        """
        {
          "content": [
            {"reportReference":  "1000002"}
          ]
        }
        """,
        JsonCompareMode.LENIENT,
      )
  }

  @Test
  fun `SAR endpoint returns no content if prisoner number is not found`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=A0003BB")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isNoContent
  }

  @ParameterizedTest(name = "SAR date range {0} to {1} selects on the incident date, not when last changed: {2}")
  @CsvSource(
    value = [
      "2023-11-01 | 2023-11-01 | true",
      "2023-10-31 | 2023-11-02 | true",
      "2023-12-05 | 2023-12-05 | false",
      "2023-12-01 |            | false",
    ],
    delimiter = '|',
  )
  fun `SAR date range selects on the incident date`(
    fromDate: String,
    toDate: String?,
    expectReportReturned: Boolean,
  ) {
    // reported, created and last changed today, but about an incident a month earlier
    reportRepository.deleteAll()
    reportRepository.save(
      buildReport(
        reportReference = "1000003",
        reportTime = now,
        generatePrisonerInvolvement = 1,
      ).apply { incidentDateAndTime = LocalDateTime.parse("2023-11-01T23:30:00") },
    )

    val url = "/subject-access-request?prn=A0001AA&fromDate=$fromDate" + (toDate?.let { "&toDate=$it" } ?: "")
    webTestClient.get()
      .uri(url)
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().run {
        if (expectReportReturned) {
          isOk
            .expectBody().jsonPath("content[0].reportReference").isEqualTo("1000003")
        } else {
          isNoContent
        }
      }
  }

  @Test
  fun `SAR endpoint marks which prisoner the request is about`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=A0002AA")
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isOk
      .expectBody().json(
        // language=json
        """
        {
          "content": [
            {
              "reportReference": "1000002",
              "prisonersInvolved": [
                {"prisonerNumber": "A0001AA", "subjectOfRequest": false},
                {"prisonerNumber": "A0002AA", "subjectOfRequest": true}
              ]
            }
          ]
        }
        """,
        JsonCompareMode.LENIENT,
      )
  }

  @ParameterizedTest(name = "SAR endpoint for date range {0} to {1} should return reports? {2}")
  @CsvSource(
    value = [
      "           |            | true",
      "2023-12-05 |            | true",
      "2023-12-06 |            | false",
      "           | 2023-12-05 | true",
      "           | 2023-12-04 | false",
      "2023-12-04 | 2023-12-04 | false",
      "2023-12-06 | 2023-12-06 | false",
      "2023-12-05 | 2023-12-05 | true",
      "2023-12-04 | 2023-12-06 | true",
    ],
    delimiter = '|',
  )
  fun `SAR endpoint for date range`(
    fromDate: String?,
    toDate: String?,
    expectReportReturned: Boolean,
  ) {
    val url = buildString {
      append("/subject-access-request?prn=A0002AA")
      if (fromDate != null) {
        append("&fromDate=$fromDate")
      }
      if (toDate != null) {
        append("&toDate=$toDate")
      }
    }

    webTestClient.get()
      .uri(url)
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().run {
        if (expectReportReturned) {
          isOk
            .expectBody().jsonPath("content").value<List<Any>> {
              assertThat(it).hasSize(1)
            }
        } else {
          isNoContent
        }
      }
  }
}
