package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.Clock

@DisplayName("Report resource")
class ReportResourceTest : SqsIntegrationTestBase() {

  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Autowired
  lateinit var reportRepository: ReportRepository

  @Autowired
  lateinit var eventRepository: EventRepository

  lateinit var existingReport: Report

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    existingReport = reportRepository.save(
      buildIncidentReport(
        incidentNumber = "IR-0000000001124143",
        reportTime = now,
      ),
    )
  }

  @DisplayName("GET /incident-reports")
  @Nested
  inner class GetReports {
    private val url = "/incident-reports"

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.get().uri(url),
      "VIEW_INCIDENT_REPORTS",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get too large a page of reports`() {
        webTestClient.get().uri("$url?size=100")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Page size must be")
          }
      }

      @Test
      fun `cannot sort by invalid property`() {
        webTestClient.get().uri("$url?sort=missing,DESC")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("No property 'missing' found for type 'Report'")
          }
      }

      @ParameterizedTest(name = "cannot filter by invalid `{0}`")
      @ValueSource(
        strings = [
          "prisonId=",
          "prisonId=M",
          "prisonId=Moorland+(HMP)",
          "source=nomis",
          "status=new",
          "type=ABSCOND",
          "incidentDateFrom=2024",
          "incidentDateUntil=yesterday",
          "reportedDateFrom=1%2F1%2F2020",
        ],
      )
      fun `cannot filter by invalid property`(param: String) {
        webTestClient.get().uri("$url?$param")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      // NB: JSON response assertions are deliberately non-strict to keep the code shorter;
      // tests for single report responses check JSON much more thoroughly

      @Test
      fun `returns empty list when there are no reports`() {
        reportRepository.deleteAll()
        eventRepository.deleteAll()

        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """{
              "content": [],
              "number": 0,
              "size": 20,
              "numberOfElements": 0,
              "totalElements": 0,
              "totalPages": 0,
              "sort": ["incidentDateAndTime,DESC"]
            }""",
            true,
          )
      }

      @Test
      fun `can get first page of reports`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """{
              "content": [{
                "id": "${existingReport.id}",
                "incidentNumber": "IR-0000000001124143",
                "incidentDateAndTime": "2023-12-05T11:34:56"
              }],
              "number": 0,
              "size": 20,
              "numberOfElements": 1,
              "totalElements": 1,
              "totalPages": 1,
              "sort": ["incidentDateAndTime,DESC"]
            }""",
            false,
          )
      }

      @DisplayName("when many reports exist")
      @Nested
      inner class WhenManyReportsExist {
        @BeforeEach
        fun setUp() {
          // makes an additional 4 older reports, 2 of which are from NOMIS
          reportRepository.saveAll(
            listOf("IR-0000000001017203", "IR-0000000001006603", "94728", "31934")
              .mapIndexed { index, incidentNumber ->
                val fromDps = incidentNumber.startsWith("IR-")
                val daysBefore = index.toLong() + 1
                buildIncidentReport(
                  incidentNumber = incidentNumber,
                  reportTime = now.minusDays(daysBefore),
                  prisonId = if (index < 2) "LEI" else "MDI",
                  status = if (fromDps) Status.DRAFT else Status.AWAITING_ANALYSIS,
                  source = if (fromDps) InformationSource.DPS else InformationSource.NOMIS,
                  type = if (index < 3) Type.FINDS else Type.FIRE,
                )
              },
          )
        }

        @Test
        fun `can get first page of reports`() {
          webTestClient.get().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              """{
                "content": [
                  {
                    "incidentNumber": "IR-0000000001124143",
                    "incidentDateAndTime": "2023-12-05T11:34:56"
                  },
                  {
                    "incidentNumber": "IR-0000000001017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56"
                  },
                  {
                    "incidentNumber": "IR-0000000001006603",
                    "incidentDateAndTime": "2023-12-03T11:34:56"
                  },
                  {
                    "incidentNumber": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56"
                  },
                  {
                    "incidentNumber": "31934",
                    "incidentDateAndTime": "2023-12-01T11:34:56"
                  }
                ],
                "number": 0,
                "size": 20,
                "numberOfElements": 5,
                "totalElements": 5,
                "totalPages": 1,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              false,
            )
        }

        @Test
        fun `can choose a page size`() {
          webTestClient.get().uri("$url?size=2")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              """{
                "content": [
                  {
                    "incidentNumber": "IR-0000000001124143",
                    "incidentDateAndTime": "2023-12-05T11:34:56"
                  },
                  {
                    "incidentNumber": "IR-0000000001017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56"
                  }
                ],
                "number": 0,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              false,
            )
        }

        @Test
        fun `can get another page of reports`() {
          webTestClient.get().uri("$url?size=2&page=1")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              """{
                "content": [
                  {
                    "incidentNumber": "IR-0000000001006603",
                    "incidentDateAndTime": "2023-12-03T11:34:56"
                  },
                  {
                    "incidentNumber": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56"
                  }
                ],
                "number": 1,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              false,
            )
        }

        @ParameterizedTest(name = "can sort reports by {0}")
        @ValueSource(
          strings = [
            "incidentDateAndTime,ASC",
            "incidentDateAndTime,DESC",
            "incidentNumber,ASC",
            "incidentNumber,DESC",
            "id,ASC",
            "id,DESC",
          ],
        )
        fun `can sort reports`(sortParam: String) {
          val expectedIncidentNumbers = mapOf(
            "incidentDateAndTime,ASC" to listOf("31934", "94728", "IR-0000000001006603", "IR-0000000001017203", "IR-0000000001124143"),
            "incidentDateAndTime,DESC" to listOf("IR-0000000001124143", "IR-0000000001017203", "IR-0000000001006603", "94728", "31934"),
            "incidentNumber,ASC" to listOf("31934", "94728", "IR-0000000001006603", "IR-0000000001017203", "IR-0000000001124143"),
            "incidentNumber,DESC" to listOf("IR-0000000001124143", "IR-0000000001017203", "IR-0000000001006603", "94728", "31934"),
            // id, being a UUIDv7, should follow table insertion order (i.e. what setUp methods do above)
            "id,ASC" to listOf("IR-0000000001124143", "IR-0000000001017203", "IR-0000000001006603", "94728", "31934"),
            "id,DESC" to listOf("31934", "94728", "IR-0000000001006603", "IR-0000000001017203", "IR-0000000001124143"),
          )[sortParam]!!

          webTestClient.get().uri("$url?sort=$sortParam")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              """{
                "number": 0,
                "size": 20,
                "numberOfElements": 5,
                "totalElements": 5,
                "totalPages": 1,
                "sort": ["$sortParam"]
              }""",
              false,
            ).jsonPath("content[*].incidentNumber").value<List<String>> {
              assertThat(it).isEqualTo(expectedIncidentNumbers)
            }
        }

        @ParameterizedTest(name = "can filter reports by `{0}`")
        @CsvSource(
          value = [
            "''                                                       | 5",
            "prisonId=MDI                                             | 3",
            "status=DRAFT                                             | 3",
            "status=AWAITING_ANALYSIS                                 | 2",
            "status=AWAITING_ANALYSIS,DRAFT                           | 5",
            "status=AWAITING_ANALYSIS&status=DRAFT                    | 5",
            "status=AWAITING_ANALYSIS,DRAFT&source=DPS                | 3",
            "status=CLOSED                                            | 0",
            "source=DPS                                               | 3",
            "source=NOMIS                                             | 2",
            "source=NOMIS&prisonId=MDI                                | 2",
            "source=DPS&prisonId=MDI                                  | 1",
            "type=ASSAULT                                             | 0",
            "type=FIRE                                                | 1",
            "type=FIRE&prisonId=LEI                                   | 0",
            "type=FIRE&prisonId=MDI                                   | 1",
            "incidentDateFrom=2023-12-05                              | 1",
            "incidentDateFrom=2023-12-04                              | 2",
            "incidentDateFrom=2023-12-03                              | 3",
            "incidentDateUntil=2023-12-03                             | 3",
            "incidentDateUntil=2023-12-02                             | 2",
            "incidentDateFrom=2023-12-02&incidentDateUntil=2023-12-02 | 1",
          ],
          delimiter = '|',
        )
        fun `can filter reports`(params: String, count: Int) {
          webTestClient.get().uri("$url?$params")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("totalElements").isEqualTo(count)
        }
      }
    }
  }

  @DisplayName("GET /incident-reports/{id}")
  @Nested
  inner class GetBasicReportById {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.get().uri(url),
      "VIEW_INCIDENT_REPORTS",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by ID if it is not found`() {
        webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can get a report by ID`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-reports/{id}/with-details")
  @Nested
  inner class GetReportWithDetailsById {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}/with-details"
    }

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.get().uri(url),
      "VIEW_INCIDENT_REPORTS",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by ID if it is not found`() {
        webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555/with-details")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can get a report by ID`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124143",
              "description": "A new incident created in the new service of type Finds",
              "event": {
                "eventId": "IE-0000000001124143",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An event occurred",
                "description": "Details of the event",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-reports/incident-number/{incident-number}")
  @Nested
  inner class GetBasicReportByIncidentNumber {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/incident-number/${existingReport.incidentNumber}"
    }

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.get().uri(url),
      "VIEW_INCIDENT_REPORTS",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by incident number if it is not found`() {
        webTestClient.get().uri("/incident-reports/incident-number/IR-11111111")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can get a report by incident number`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-reports/incident-number/{incident-number}/with-details")
  @Nested
  inner class GetReportWithDetailsByIncidentNumber {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/incident-number/${existingReport.incidentNumber}/with-details"
    }

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.get().uri(url),
      "VIEW_INCIDENT_REPORTS",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by incident number if it is not found`() {
        webTestClient.get().uri("/incident-reports/incident-number/IR-11111111/with-details")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can get a report by incident number`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124143",
              "description": "A new incident created in the new service of type Finds",
              "event": {
                "eventId": "IE-0000000001124143",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An event occurred",
                "description": "Details of the event",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("POST /incident-reports")
  @Nested
  inner class CreateReport {
    private val url = "/incident-reports"

    val createReportRequest = CreateReportRequest(
      incidentDateAndTime = now.minusHours(1),
      title = "An incident occurred",
      description = "Longer explanation of incident",
      type = Type.SELF_HARM,
      prisonId = "MDI",
      reportedBy = "user2",
      reportedAt = now,
      createNewEvent = true,
    )

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.post().uri(url).bodyValue(createReportRequest.toJson()),
      "MAINTAIN_INCIDENT_REPORTS",
      "write",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot create a report with invalid payload`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue("{}")
          .exchange()
          .expectStatus().isBadRequest

        assertNoDomainMessagesSent()
      }

      @ParameterizedTest(name = "cannot create a report with invalid `{0}` field")
      @ValueSource(strings = ["prisonId", "title", "reportedBy"])
      fun `cannot create a report with invalid fields`(fieldName: String) {
        val invalidPayload = createReportRequest.copy(
          prisonId = if (fieldName == "prisonId") "" else createReportRequest.prisonId,
          title = if (fieldName == "title") "" else createReportRequest.title,
          reportedBy = if (fieldName == "reportedBy") "" else createReportRequest.reportedBy,
        ).toJson()
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains(fieldName)
          }

        assertNoDomainMessagesSent()
      }

      @Test
      fun `cannot create a report with invalid dates`() {
        val invalidPayload = createReportRequest.copy(
          incidentDateAndTime = now.minusMinutes(10),
          reportedAt = now.minusMinutes(20),
        ).toJson()
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("incidentDateAndTime must be before reportedAt")
          }

        assertNoDomainMessagesSent()
      }

      @Test
      fun `cannot create a report without creating or linking event`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.copy(createNewEvent = false).toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Either createNewEvent or linkedEventId must be provided")
          }

        assertNoDomainMessagesSent()
      }

      @Test
      fun `cannot create a report with an inactive type`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.copy(type = Type.OLD_ASSAULT).toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Inactive incident type OLD_ASSAULT")
          }

        assertNoDomainMessagesSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can add a new incident report`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.toJson())
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
            {
              "type": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An incident occurred",
              "description": "Longer explanation of incident",
              "event": {
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An incident occurred",
                "description": "Longer explanation of incident",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "INCIDENT_REPORTING_API"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "INCIDENT_REPORTING_API"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "user2",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "user2",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false
            }
            """,
            false,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source })
            .containsExactlyInAnyOrder(
              "incident.report.created" to InformationSource.DPS,
            )
        }
      }

      @Test
      fun `can add a new incident linked to an existing event`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            createReportRequest.copy(
              createNewEvent = false,
              linkedEventId = existingReport.event.eventId,
            ).toJson(),
          )
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
            {
              "type": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An incident occurred",
              "description": "Longer explanation of incident",
              "event": {
                "eventId": "${existingReport.event.eventId}",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An event occurred",
                "description": "Details of the event",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "INCIDENT_REPORTING_API"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "user2",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "user2",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false
            }
            """,
            false,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source })
            .containsExactlyInAnyOrder(
              "incident.report.created" to InformationSource.DPS,
            )
        }
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}")
  @Nested
  inner class UpdateReport {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.patch().uri(url).bodyValue(UpdateReportRequest().toJson()),
      "MAINTAIN_INCIDENT_REPORTS",
      "write",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot update a report with invalid payload`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue("[]")
          .exchange()
          .expectStatus().isBadRequest

        assertNoDomainMessagesSent()
      }

      @ParameterizedTest(name = "cannot update a report with invalid `{0}` field")
      @ValueSource(strings = ["prisonId", "title", "reportedBy"])
      fun `cannot update a report with invalid fields`(fieldName: String) {
        val invalidPayload = UpdateReportRequest(
          prisonId = if (fieldName == "prisonId") "" else null,
          title = if (fieldName == "title") "" else null,
          reportedBy = if (fieldName == "reportedBy") "" else null,
        ).toJson()
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload.toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains(fieldName)
          }

        assertNoDomainMessagesSent()
      }

      @Test
      fun `cannot update a report with invalid dates`() {
        val invalidPayload = UpdateReportRequest(
          incidentDateAndTime = now.minusMinutes(10),
          reportedAt = now.minusMinutes(20),
        ).toJson()
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("incidentDateAndTime must be before reportedAt")
          }

        assertNoDomainMessagesSent()
      }

      @Test
      fun `cannot update a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(UpdateReportRequest().toJson())
          .exchange()
          .expectStatus().isNotFound

        assertNoDomainMessagesSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can update an incident report with no changes`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(UpdateReportRequest().toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false
            }
            """,
            true,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source })
            .containsExactlyInAnyOrder(
              "incident.report.amended" to InformationSource.DPS,
            )
        }
      }

      @Test
      fun `can update all incident report fields`() {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = now.minusHours(2),
          prisonId = "LEI",
          title = "Updated report IR-0000000001124143",
          description = "Updated incident report of type Finds",
          reportedBy = "different-user",
          reportedAt = now.minusMinutes(2),
        )
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T10:34:56",
              "prisonId": "LEI",
              "title": "Updated report IR-0000000001124143",
              "description": "Updated incident report of type Finds",
              "reportedBy": "different-user",
              "reportedAt": "2023-12-05T12:32:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false
            }
            """,
            true,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source })
            .containsExactlyInAnyOrder(
              "incident.report.amended" to InformationSource.DPS,
            )
        }
      }

      @ParameterizedTest(name = "can update `{0}` of an incident report")
      @ValueSource(strings = ["incidentDateAndTime", "prisonId", "title", "description", "reportedBy", "reportedAt"])
      fun `can update an incident report field`(fieldName: String) {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = if (fieldName == "incidentDateAndTime") now.minusHours(2) else null,
          prisonId = if (fieldName == "prisonId") "LEI" else null,
          title = if (fieldName == "title") "Updated report IR-0000000001124143" else null,
          description = if (fieldName == "description") "Updated incident report of type Finds" else null,
          reportedBy = if (fieldName == "reportedBy") "different-user" else null,
          reportedAt = if (fieldName == "reportedAt") now.minusMinutes(2) else null,
        )
        val expectedIncidentDateAndTime = if (fieldName == "incidentDateAndTime") {
          "2023-12-05T10:34:56"
        } else {
          "2023-12-05T11:34:56"
        }
        val expectedPrisonId = if (fieldName == "prisonId") {
          "LEI"
        } else {
          "MDI"
        }
        val expectedTitle = if (fieldName == "title") {
          "Updated report IR-0000000001124143"
        } else {
          "Incident Report IR-0000000001124143"
        }
        val expectedDescription = if (fieldName == "description") {
          "Updated incident report of type Finds"
        } else {
          "A new incident created in the new service of type Finds"
        }
        val expectedReportedBy = if (fieldName == "reportedBy") {
          "different-user"
        } else {
          "USER1"
        }
        val expectedReportedAt = if (fieldName == "reportedAt") {
          "2023-12-05T12:32:56"
        } else {
          "2023-12-05T12:34:56"
        }
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "incidentNumber": "IR-0000000001124143",
              "type": "FINDS",
              "incidentDateAndTime": "$expectedIncidentDateAndTime",
              "prisonId": "$expectedPrisonId",
              "title": "$expectedTitle",
              "description": "$expectedDescription",
              "reportedBy": "$expectedReportedBy",
              "reportedAt": "$expectedReportedAt",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false
            }
            """,
            true,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source })
            .containsExactlyInAnyOrder(
              "incident.report.amended" to InformationSource.DPS,
            )
        }
      }
    }
  }

  @DisplayName("DELETE /incident-reports/{id}")
  @Nested
  inner class DeleteReportById {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @TestFactory
    fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
      webTestClient.delete().uri(url),
      "MAINTAIN_INCIDENT_REPORTS",
      "write",
    )

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot delete a report by ID if it is not found`() {
        webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound

        assertNoDomainMessagesSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @ParameterizedTest(name = "can delete a report by ID (including orphaned event? {0})")
      @ValueSource(booleans = [true, false])
      fun `can delete a report by ID`(deleteOrphanedEvents: Boolean) {
        val reportId = existingReport.id!!
        val eventId = existingReport.event.id!!

        webTestClient.delete().uri("$url?deleteOrphanedEvents=$deleteOrphanedEvents")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "$reportId",
              "incidentNumber": "IR-0000000001124143"
            }
            """,
            false,
          )

        assertThat(reportRepository.findOneEagerlyById(reportId)).isNull()
        if (deleteOrphanedEvents) {
          assertThat(eventRepository.findById(eventId)).isEmpty
        } else {
          assertThat(eventRepository.findById(eventId)).isPresent
        }

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source })
            .containsExactlyInAnyOrder(
              "incident.report.deleted" to InformationSource.DPS,
            )
        }
      }

      @Test
      fun `cannot cascade deleting non-orphan event`() {
        val reportId = existingReport.id!!
        val eventId = existingReport.event.id!!

        existingReport.event.addReport(
          buildIncidentReport(
            incidentNumber = "IR-0000000001124142",
            reportTime = now.minusMinutes(5),
          ),
        )
        eventRepository.save(existingReport.event)
        val otherReportId = reportRepository.findAll().first { it.id != reportId }.id!!

        webTestClient.delete().uri("$url?deleteOrphanedEvents=true")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "$reportId",
              "incidentNumber": "IR-0000000001124143"
            }
            """,
            false,
          )

        val remainingReportIds = reportRepository.findAllById(listOf(reportId, otherReportId)).map { it.id }
        assertThat(remainingReportIds).containsOnly(otherReportId)
        assertThat(eventRepository.findById(eventId)).isPresent
      }
    }
  }
}
