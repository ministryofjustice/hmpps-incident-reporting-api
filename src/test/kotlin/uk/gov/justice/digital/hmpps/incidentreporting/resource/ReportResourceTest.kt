package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
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
import org.springframework.test.util.JsonExpectationsHelper
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
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.time.Clock
import java.util.UUID

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

  protected fun assertThatReportWasModified(id: UUID) {
    webTestClient.get().uri("/incident-reports/$id")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectBody().json(
        // language=json
        """
          {
            "modifiedAt": "2023-12-05T12:34:56",
            "modifiedBy": "INCIDENT_REPORTING_API"
          }
          """,
        false,
      )
  }

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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.post().uri(url).bodyValue(createReportRequest.toJson()),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot create a report with invalid payload`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            "{}",
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
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

        assertThatNoDomainEventsWereSent()
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

        assertThatNoDomainEventsWereSent()
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

        assertThatNoDomainEventsWereSent()
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

        assertThatNoDomainEventsWereSent()
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

        assertThatDomainEventWasSent("incident.report.created", null)
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

        assertThatDomainEventWasSent("incident.report.created", null)
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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.patch().uri(url).bodyValue(UpdateReportRequest().toJson()),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot update a report with invalid payload`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            "[]",
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
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

        assertThatNoDomainEventsWereSent()
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

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot update a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(UpdateReportRequest().toJson())
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
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

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
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

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
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

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
      }

      @ParameterizedTest(name = "can propagate updates to parent event when requested: {0}")
      @ValueSource(booleans = [true, false])
      fun `can propagate updates to parent event when requested`(updateEvent: Boolean) {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = now.minusHours(2),
          prisonId = "LEI",
          title = "Updated report IR-0000000001124143",
          description = "Updated incident report of type Finds",
          reportedBy = "different-user",
          reportedAt = now.minusMinutes(2),

          updateEvent = updateEvent,
        )
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk

        val eventJson = eventRepository.findOneByEventId("IE-0000000001124143")!!
          .toDto().toJson()
        JsonExpectationsHelper().assertJsonEqual(
          if (updateEvent) {
            // language=json
            """
            {
              "eventId": "IE-0000000001124143",
              "eventDateAndTime": "2023-12-05T10:34:56",
              "prisonId": "LEI",
              "title": "Updated report IR-0000000001124143",
              "description": "Updated incident report of type Finds",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API"
            }
            """
          } else {
            // language=json
            """
            {
              "eventId": "IE-0000000001124143",
              "eventDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An event occurred",
              "description": "Details of the event",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1"
            }
            """
          },
          eventJson,
          false,
        )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}/status")
  @Nested
  inner class ChangeStatus {
    private lateinit var url: String

    // language=json
    private val validPayload = """
      {"newStatus": "AWAITING_ANALYSIS"}
    """

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}/status"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.patch().uri(url).bodyValue(validPayload),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot change status of a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555/status")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change status of a report with invalid payload`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"status": "CLOSED"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change status of a report to an unknown one`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newStatus": "OPEN"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can change status of a report but keeping it the same`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newStatus": "DRAFT"}
            """,
          )
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
              "createdInNomis": false,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                }
              ]
            }
            """,
            false,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can change status of a report preserving history`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
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
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                },
                {
                  "status": "AWAITING_ANALYSIS",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "INCIDENT_REPORTING_API"
                }
              ]
            }
            """,
            false,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124143",
          InformationSource.DPS,
          WhatChanged.STATUS,
        )
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}/type")
  @Nested
  inner class ChangeType {
    private lateinit var urlWithNoQuestionsOrHistory: String

    // language=json
    private val validPayload = """
      {"newType": "DAMAGE"}
    """

    @BeforeEach
    fun setUp() {
      urlWithNoQuestionsOrHistory = "/incident-reports/${existingReport.id}/type"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory).bodyValue(validPayload),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot change type of a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change type of a report with invalid payload`() {
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"type": "DAMAGE"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change type of a report to an unknown one`() {
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newType": "MISSING_TYPE"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change type of a report to inactive one`() {
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newType": "OLD_ASSAULT3"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Inactive incident type OLD_ASSAULT3")
          }

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can change type of a report but keeping it the same`() {
        val reportWithQuestions = reportRepository.save(
          buildIncidentReport(
            incidentNumber = "IR-0000000001124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_ANALYSIS,
            generateQuestions = 2,
            generateResponses = 2,
            generateHistory = 0,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${reportWithQuestions.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newType": "FINDS"}
            """,
          )
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${reportWithQuestions.id}",
              "incidentNumber": "IR-0000000001124146",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:31:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124146",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56",
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:31:56",
              "modifiedAt": "2023-12-05T12:31:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "history": [],
              "questions": [
                {
                  "code": "QID-000000000001",
                  "question": "Question #1",
                  "additionalInformation": "Explanation #1",
                  "responses": [
                    {
                      "response": "Response #1",
                      "additionalInformation": "Prose #1",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    },
                    {
                      "response": "Response #2",
                      "additionalInformation": "Prose #2",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    }
                  ]
                },
                {
                  "code": "QID-000000000002",
                  "question": "Question #2",
                  "additionalInformation": "Explanation #2",
                  "responses": [
                    {
                      "response": "Response #1",
                      "additionalInformation": "Prose #1",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    },
                    {
                      "response": "Response #2",
                      "additionalInformation": "Prose #2",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    }
                  ]
                }
              ]
            }
            """,
            false,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can change type of a report creating history when there was none`() {
        val reportWithQuestions = reportRepository.save(
          buildIncidentReport(
            incidentNumber = "IR-0000000001124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_ANALYSIS,
            generateQuestions = 2,
            generateResponses = 2,
            generateHistory = 0,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${reportWithQuestions.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${reportWithQuestions.id}",
              "incidentNumber": "IR-0000000001124146",
              "type": "DAMAGE",
              "incidentDateAndTime": "2023-12-05T11:31:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124146",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56",
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:31:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false,
              "history": [
                {
                  "type": "FINDS",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "INCIDENT_REPORTING_API",
                  "questions": [
                    {
                      "code": "QID-000000000001",
                      "question": "Question #1",
                      "additionalInformation": "Explanation #1",
                      "responses": [
                        {
                          "response": "Response #1",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        },
                        {
                          "response": "Response #2",
                          "additionalInformation": "Prose #2",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    },
                    {
                      "code": "QID-000000000002",
                      "question": "Question #2",
                      "additionalInformation": "Explanation #2",
                      "responses": [
                        {
                          "response": "Response #1",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        },
                        {
                          "response": "Response #2",
                          "additionalInformation": "Prose #2",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    }
                  ]
                }
              ],
              "questions": []
            }
            """,
            false,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124146",
          InformationSource.DPS,
          WhatChanged.TYPE,
        )
      }

      @Test
      fun `can change type of a report preserving history when it already existed`() {
        val reportWithQuestionsAndHistory = reportRepository.save(
          buildIncidentReport(
            incidentNumber = "IR-0000000001124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_ANALYSIS,
            generateQuestions = 1,
            generateResponses = 1,
            generateHistory = 1,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${reportWithQuestionsAndHistory.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${reportWithQuestionsAndHistory.id}",
              "incidentNumber": "IR-0000000001124146",
              "type": "DAMAGE",
              "incidentDateAndTime": "2023-12-05T11:31:56",
              "prisonId": "MDI",
              "title": "Incident Report IR-0000000001124146",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56",
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:31:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false,
              "history": [
                {
                  "type": "ASSAULT",
                  "changedAt": "2023-12-05T12:31:56",
                  "changedBy": "some-past-user",
                  "questions": [
                    {
                      "code": "QID-1-000000000001",
                      "question": "Historical question #1-1",
                      "additionalInformation": "Explanation #1 in history #1",
                      "responses": [
                        {
                          "response": "Historical response #1-1",
                          "additionalInformation": "Prose #1 in history #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    }
                  ]
                },
                {
                  "type": "FINDS",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "INCIDENT_REPORTING_API",
                  "questions": [
                    {
                      "code": "QID-000000000001",
                      "question": "Question #1",
                      "additionalInformation": "Explanation #1",
                      "responses": [
                        {
                          "response": "Response #1",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    }
                  ]
                }
              ],
              "questions": []
            }
            """,
            false,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "IR-0000000001124146",
          InformationSource.DPS,
          WhatChanged.TYPE,
        )
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
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.delete().uri(url),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

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

        assertThatNoDomainEventsWereSent()
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

        assertThatDomainEventWasSent("incident.report.deleted", "IR-0000000001124143")
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

  @Nested
  abstract inner class RelatedObjects(val urlSuffix: String, val whatChanged: WhatChanged? = null) {
    protected lateinit var existingReportWithRelatedObjects: Report
    protected lateinit var urlWithRelatedObjects: String
    protected lateinit var urlWithoutRelatedObjects: String
    protected lateinit var urlForFirstRelatedObject: String
    protected lateinit var urlForSecondRelatedObject: String

    @BeforeEach
    fun setUp() {
      urlWithoutRelatedObjects = "/incident-reports/${existingReport.id}/$urlSuffix"

      existingReportWithRelatedObjects = reportRepository.save(
        buildIncidentReport(
          incidentNumber = "IR-0000000001124146",
          reportTime = now,
          generateStaffInvolvement = 2,
          generatePrisonerInvolvement = 2,
          generateLocations = 2,
          generateCorrections = 2,
          generateEvidence = 2,
        ),
      )
      urlWithRelatedObjects = "/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix"
      urlForFirstRelatedObject = "/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/1"
      urlForSecondRelatedObject = "/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/2"
    }

    abstract inner class ListObjects {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(urlWithRelatedObjects),
          "VIEW_INCIDENT_REPORTS",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot list objects for a report if it is not found`() {
          webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can list objects for a report when there are none`() {
          webTestClient.get().uri(urlWithoutRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              true,
            )
        }

        @Test
        fun `can list objects for a report when there are two`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/list-response.json")
          webTestClient.get().uri(urlWithRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )
        }
      }
    }

    abstract inner class AddObject(
      val invalidRequests: List<InvalidRequestTestCase> = emptyList(),
    ) {
      val validRequest = getResource("/related-objects/$urlSuffix/add-request.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.post().uri(urlWithRelatedObjects).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot add object to a report if it is not found`() {
          webTestClient.post().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @DisplayName("cannot add invalid object to a report")
        @TestFactory
        fun `cannot add invalid object to a report`(): List<DynamicTest> {
          val requests = mutableListOf(
            InvalidRequestTestCase(
              "empty request",
              // language=json
              "{}",
            ),
            InvalidRequestTestCase(
              "invalid shape",
              // language=json
              "[]",
            ),
          )
          requests.addAll(invalidRequests)
          return requests.map { (name, request) ->
            DynamicTest.dynamicTest(name) {
              webTestClient.post().uri(urlWithoutRelatedObjects)
                .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest

              assertThatNoDomainEventsWereSent()
            }
          }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can add first object to a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/add-response-one.json")
          webTestClient.post().uri(urlWithoutRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReport.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124143",
            InformationSource.DPS,
            whatChanged,
          )
        }

        @Test
        fun `can another object to a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/add-response-many.json")
          webTestClient.post().uri(urlWithRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "IR-0000000001124146", InformationSource.DPS, whatChanged)
        }
      }
    }

    abstract inner class UpdateObject(
      val invalidRequests: List<InvalidRequestTestCase> = emptyList(),
      val nullablePropertyRequests: List<NullablePropertyTestCase> = emptyList(),
    ) {
      val validRequest = getResource("/related-objects/$urlSuffix/update-request.json")
      val validPartialRequest = getResource("/related-objects/$urlSuffix/update-request-partial.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.patch().uri(urlForFirstRelatedObject).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot update related object for a report if it is not found`() {
          webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix/1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot update related object at index 0`() {
          webTestClient.patch().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/0")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 0 not found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot update related object beyond end`() {
          webTestClient.patch().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/3")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 3 not found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @DisplayName("cannot update related object with invalid payload")
        @TestFactory
        fun `cannot update related object with invalid payload`(): List<DynamicTest> {
          val requests = mutableListOf(
            InvalidRequestTestCase(
              "invalid shape",
              // language=json
              "[]",
            ),
          )
          requests.addAll(invalidRequests)
          return requests.map { (name, request) ->
            DynamicTest.dynamicTest(name) {
              webTestClient.patch().uri(urlForFirstRelatedObject)
                .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest

              assertThatNoDomainEventsWereSent()
            }
          }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @ParameterizedTest(name = "can update {0} related object with no changes")
        @ValueSource(strings = ["first", "second"])
        fun `can update a related object with no changes`(index: String) {
          val expectedResponse = getResource("/related-objects/$urlSuffix/list-response.json")
          val url = if (index == "first") {
            urlForFirstRelatedObject
          } else {
            urlForSecondRelatedObject
          }
          webTestClient.patch().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(
              // language=json
              "{}",
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "IR-0000000001124146", InformationSource.DPS, whatChanged)
        }

        @ParameterizedTest(name = "can update {0} related object fully")
        @ValueSource(strings = ["first", "second"])
        fun `can update a related object fully`(index: String) {
          val expectedResponse = getResource("/related-objects/$urlSuffix/update-response-$index.json")
          val url = if (index == "first") {
            urlForFirstRelatedObject
          } else {
            urlForSecondRelatedObject
          }
          webTestClient.patch().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "IR-0000000001124146", InformationSource.DPS, whatChanged)
        }

        @ParameterizedTest(name = "can update {0} related object partially")
        @ValueSource(strings = ["first", "second"])
        fun `can update a related object partially`(index: String) {
          val expectedResponse = getResource("/related-objects/$urlSuffix/update-response-$index-partial.json")
          val url = if (index == "first") {
            urlForFirstRelatedObject
          } else {
            urlForSecondRelatedObject
          }
          webTestClient.patch().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validPartialRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "IR-0000000001124146", InformationSource.DPS, whatChanged)
        }

        @DisplayName("can update nullable properties")
        @TestFactory
        fun `can update nullable properties`(): List<DynamicTest> {
          return nullablePropertyRequests.flatMap { testCase ->
            testCase.makeValidRequestTests().map { (name, request, expectedFieldValue) ->
              DynamicTest.dynamicTest(name) {
                webTestClient.patch().uri(urlForFirstRelatedObject)
                  .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                  .header("Content-Type", "application/json")
                  .bodyValue(request)
                  .exchange()
                  .expectStatus().isOk
                  .expectBody().jsonPath("[0].${testCase.field}").isEqualTo(expectedFieldValue)
              }
            }
          }
        }
      }
    }

    abstract inner class RemoveObject {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.delete().uri(urlForFirstRelatedObject),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot delete related object from a report if it is not found`() {
          webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix/1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete related object at index 0`() {
          webTestClient.delete().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/0")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 0 not found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete related object beyond end`() {
          webTestClient.delete().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/3")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 3 not found")
            }

          assertThatNoDomainEventsWereSent()
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can delete first related object from a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/delete-response-first.json")
          webTestClient.delete().uri(urlForFirstRelatedObject)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "IR-0000000001124146", InformationSource.DPS, whatChanged)
        }

        @Test
        fun `can delete second related object from a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/delete-response-second.json")
          webTestClient.delete().uri(urlForSecondRelatedObject)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "IR-0000000001124146", InformationSource.DPS, whatChanged)
        }
      }
    }
  }

  @DisplayName("Staff involvement")
  @Nested
  inner class StaffInvolvement : RelatedObjects("staff-involved", WhatChanged.STAFF_INVOLVED) {
    @DisplayName("GET /incident-reports/{reportId}/staff-involved")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/staff-involved")
    @Nested
    inner class AddObject : RelatedObjects.AddObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "short staff username",
          getResource("/related-objects/staff-involved/add-request-short-username.json"),
        ),
      ),
    )

    @DisplayName("PATCH /incident-reports/{reportId}/staff-involved/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "short staff username",
          getResource("/related-objects/staff-involved/update-request-short-username.json"),
        ),
      ),
      nullablePropertyRequests = listOf(
        NullablePropertyTestCase(
          field = "comment",
          validValue = "Different comment",
          unchangedValue = "Comment #1",
        ),
      ),
    )

    @DisplayName("DELETE /incident-reports/{reportId}/staff-involved/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Prisoner involvement")
  @Nested
  inner class PrisonerInvolvement : RelatedObjects("prisoners-involved", WhatChanged.PRISONERS_INVOLVED) {
    @DisplayName("GET /incident-reports/{reportId}/prisoners-involved")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/prisoners-involved")
    @Nested
    inner class AddObject : RelatedObjects.AddObject()

    @DisplayName("PATCH /incident-reports/{reportId}/prisoners-involved/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject(
      nullablePropertyRequests = listOf(
        NullablePropertyTestCase(
          field = "outcome",
          validValue = "PLACED_ON_REPORT",
          unchangedValue = "CHARGED_BY_POLICE",
        ),
        NullablePropertyTestCase(
          field = "comment",
          validValue = "Different comment",
          unchangedValue = "Comment #1",
        ),
      ),
    )

    @DisplayName("DELETE /incident-reports/{reportId}/prisoners-involved/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Locations")
  @Nested
  inner class Locations : RelatedObjects("locations", WhatChanged.LOCATIONS) {
    @DisplayName("GET /incident-reports/{reportId}/locations")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/locations")
    @Nested
    inner class AddObject : RelatedObjects.AddObject()

    @DisplayName("PATCH /incident-reports/{reportId}/locations/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject()

    @DisplayName("DELETE /incident-reports/{reportId}/locations/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Evidence")
  @Nested
  inner class Evidence : RelatedObjects("evidence", WhatChanged.EVIDENCE) {
    @DisplayName("GET /incident-reports/{reportId}/evidence")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/evidence")
    @Nested
    inner class AddObject : RelatedObjects.AddObject()

    @DisplayName("PATCH /incident-reports/{reportId}/evidence/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject()

    @DisplayName("DELETE /incident-reports/{reportId}/evidence/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Correction requests")
  @Nested
  inner class CorrectionRequests : RelatedObjects("correction-requests", WhatChanged.CORRECTION_REQUESTS) {
    @DisplayName("GET /incident-reports/{reportId}/correction-requests")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/correction-requests")
    @Nested
    inner class AddObject : RelatedObjects.AddObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "empty description of change",
          getResource("/related-objects/correction-requests/add-request-empty-description.json"),
        ),
      ),
    )

    @DisplayName("PATCH /incident-reports/{reportId}/correction-requests/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "short requester username",
          getResource("/related-objects/correction-requests/update-request-short-username.json"),
        ),
      ),
    )

    @DisplayName("DELETE /incident-reports/{reportId}/correction-requests/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Questions with responses")
  @Nested
  inner class QuestionsWithResponses {
    private lateinit var existingReportWithQuestionsAndResponses: Report
    private lateinit var urlWithoutQuestions: String
    private lateinit var urlWithQuestionsAndResponses: String

    @BeforeEach
    fun setUp() {
      urlWithoutQuestions = "/incident-reports/${existingReport.id}/questions"

      existingReportWithQuestionsAndResponses = reportRepository.save(
        buildIncidentReport(
          incidentNumber = "IR-0000000001124146",
          reportTime = now,
          generateQuestions = 2,
          generateResponses = 2,
        ),
      )
      urlWithQuestionsAndResponses = "/incident-reports/${existingReportWithQuestionsAndResponses.id}/questions"
    }

    @DisplayName("GET /incident-reports/{reportId}/questions")
    @Nested
    inner class ListQuestions {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(urlWithoutQuestions),
          "VIEW_INCIDENT_REPORTS",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot list questions and responses for a report if it is not found`() {
          webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can list questions and responses when there are none`() {
          webTestClient.get().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              true,
            )
        }

        @Test
        fun `can list questions and responses when there are several`() {
          val expectedResponse = getResource("/questions-with-responses/list-response-with-several.json")
          webTestClient.get().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )
        }

        @Test
        fun `can list 50 questions and responses`() {
          val report = reportRepository.save(
            buildIncidentReport(
              incidentNumber = "IR-0000000001124147",
              reportTime = now,
              generateQuestions = 50,
              generateResponses = 3,
            ),
          )
          webTestClient.get().uri("/incident-reports/${report.id}/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$").value<List<Map<String, Any>>> { questions ->
              assertThat(questions).hasSize(50)
              assertThat(questions).allSatisfy { question ->
                val responses = question["responses"] as List<*>
                assertThat(responses).hasSize(3)
              }
            }
        }
      }
    }

    @DisplayName("POST /incident-reports/{reportId}/questions")
    @Nested
    inner class AddQuestion {
      private val validRequest = getResource("/questions-with-responses/add-request-with-responses.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.post().uri(urlWithQuestionsAndResponses).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot add question and responses to a report if it is not found`() {
          webTestClient.post().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @DisplayName("cannot add question and responses if payload is invalid")
        @TestFactory
        fun `cannot add question and responses if payload is invalid`(): List<DynamicTest> {
          return listOf(
            InvalidRequestTestCase(
              "invalid payload",
              // language=json
              "[]",
            ),
            InvalidRequestTestCase(
              "long code",
              getResource("/questions-with-responses/add-request-long-code.json"),
            ),
            InvalidRequestTestCase(
              "empty question",
              getResource("/questions-with-responses/add-request-empty-question.json"),
            ),
            InvalidRequestTestCase(
              "empty response",
              getResource("/questions-with-responses/add-request-empty-response.json"),
            ),
          )
            .map { (name, request) ->
              DynamicTest.dynamicTest(name) {
                webTestClient.post().uri(urlWithoutQuestions)
                  .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                  .header("Content-Type", "application/json")
                  .bodyValue(request)
                  .exchange()
                  .expectStatus().isBadRequest
                  .expectBody().jsonPath("developerMessage").hasJsonPath()

                assertThatNoDomainEventsWereSent()
              }
            }
        }

        @Test
        fun `cannot add question without responses`() {
          val invalidRequest = getResource("/questions-with-responses/add-request-without-responses.json")
          webTestClient.post().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("addQuestionWithResponses.responses: size must be between 1 and 2147483647")
            }

          assertThatNoDomainEventsWereSent()
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can add question with responses to empty report`() {
          val expectedResponse = getResource("/questions-with-responses/add-response-without-responses.json")
          webTestClient.post().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReport.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124143",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses to report with existing questions`() {
          val expectedResponse = getResource("/questions-with-responses/add-response-with-responses.json")
          webTestClient.post().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses that have nullable fields`() {
          val validRequestWithNulls = getResource("/questions-with-responses/add-request-with-responses-and-null-fields.json")
          val expectedResponse = getResource("/questions-with-responses/add-response-with-responses-and-null-fields.json")
          webTestClient.post().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequestWithNulls)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }
      }
    }

    @DisplayName("DELETE /incident-reports/{reportId}/questions")
    @Nested
    inner class DeleteQuestion {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.delete().uri(urlWithQuestionsAndResponses),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot delete last question from a report if it is not found`() {
          webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete question from a report when the question list is empty`() {
          webTestClient.delete().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("Question list is empty")
            }

          assertThatNoDomainEventsWereSent()
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can delete last question from a report when there are several`() {
          val expectedResponse = getResource("/questions-with-responses/delete-response.json")
          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can delete all questions from a report`() {
          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124146",
            whatChanged = WhatChanged.QUESTIONS,
          )

          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "IR-0000000001124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }
      }
    }
  }
}

data class InvalidRequestTestCase(
  val name: String,
  val request: String,
)

data class NullablePropertyTestCase(
  val field: String,
  val validValue: String,
  val unchangedValue: String,
) {
  data class ValidRequestTestCase(
    val name: String,
    val request: String,
    val expectedFieldValue: String?,
  )

  fun makeValidRequestTests(): List<ValidRequestTestCase> = listOf(
    ValidRequestTestCase(
      "$field with not value provided",
      // language=json
      "{}",
      unchangedValue,
    ),
    ValidRequestTestCase(
      "$field with null value",
      // language=json
      """
      {"$field": null}
      """,
      null,
    ),
    ValidRequestTestCase(
      "$field with value provided",
      // language=json
      """
      {"$field": "$validValue"}
      """,
      validValue,
    ),
  )
}
