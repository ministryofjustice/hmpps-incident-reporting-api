package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.Clock
import java.time.LocalDateTime

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
        reportTime = LocalDateTime.now(clock),
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
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri(url)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }
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
              "totalPages": 0
            }""",
            false,
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
                "incidentDateAndTime": "2023-12-05T11:34:56",
                "event": {
                  "eventId": "IE-0000000001124143",
                  "eventDateAndTime": "2023-12-05T11:34:56"
                }
              }],
              "number": 0,
              "size": 20,
              "numberOfElements": 1,
              "totalElements": 1,
              "totalPages": 1
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
          listOf("IR-0000000001017203", "IR-0000000001006603", "94728", "31934")
            .forEachIndexed { daysBefore, incidentNumber ->
              reportRepository.save(
                buildIncidentReport(
                  incidentNumber = incidentNumber,
                  reportTime = LocalDateTime.now(clock).minusDays(daysBefore.toLong() + 1),
                  source = if (incidentNumber.startsWith("IR-")) {
                    InformationSource.DPS
                  } else {
                    InformationSource.NOMIS
                  },
                ),
              )
            }
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
                    "incidentDateAndTime": "2023-12-05T11:34:56",
                    "event": {
                      "eventId": "IE-0000000001124143",
                      "eventDateAndTime": "2023-12-05T11:34:56"
                    }
                  },
                  {
                    "incidentNumber": "IR-0000000001017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56",
                    "event": {
                      "eventId": "IE-0000000001017203",
                      "eventDateAndTime": "2023-12-04T11:34:56"
                    }
                  },
                  {
                    "incidentNumber": "IR-0000000001006603",
                    "incidentDateAndTime": "2023-12-03T11:34:56",
                    "event": {
                      "eventId": "IE-0000000001006603",
                      "eventDateAndTime": "2023-12-03T11:34:56"
                    }
                  },
                  {
                    "incidentNumber": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56",
                    "event": {
                      "eventId": "94728",
                      "eventDateAndTime": "2023-12-02T11:34:56"
                    }
                  },
                  {
                    "incidentNumber": "31934",
                    "incidentDateAndTime": "2023-12-01T11:34:56",
                    "event": {
                      "eventId": "31934",
                      "eventDateAndTime": "2023-12-01T11:34:56"
                    }
                  }
                ],
                "number": 0,
                "size": 20,
                "numberOfElements": 5,
                "totalElements": 5,
                "totalPages": 1
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
                    "incidentDateAndTime": "2023-12-05T11:34:56",
                    "event": {
                      "eventId": "IE-0000000001124143",
                      "eventDateAndTime": "2023-12-05T11:34:56"
                    }
                  },
                  {
                    "incidentNumber": "IR-0000000001017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56",
                    "event": {
                      "eventId": "IE-0000000001017203",
                      "eventDateAndTime": "2023-12-04T11:34:56"
                    }
                  }
                ],
                "number": 0,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3
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
                    "incidentDateAndTime": "2023-12-03T11:34:56",
                    "event": {
                      "eventId": "IE-0000000001006603",
                      "eventDateAndTime": "2023-12-03T11:34:56"
                    }
                  },
                  {
                    "incidentNumber": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56",
                    "event": {
                      "eventId": "94728",
                      "eventDateAndTime": "2023-12-02T11:34:56"
                    }
                  }
                ],
                "number": 1,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3
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
          ],
        )
        fun `can sort reports`(sortParam: String) {
          val expectedIncidentNumbers = mapOf(
            "incidentDateAndTime,ASC" to listOf("31934", "94728", "IR-0000000001006603", "IR-0000000001017203", "IR-0000000001124143"),
            "incidentDateAndTime,DESC" to listOf("IR-0000000001124143", "IR-0000000001017203", "IR-0000000001006603", "94728", "31934"),
            "incidentNumber,ASC" to listOf("31934", "94728", "IR-0000000001006603", "IR-0000000001017203", "IR-0000000001124143"),
            "incidentNumber,DESC" to listOf("IR-0000000001124143", "IR-0000000001017203", "IR-0000000001006603", "94728", "31934"),
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
                "totalPages": 1
              }""",
              false,
            ).jsonPath("content[*].incidentNumber")
            .value<List<String>> {
              assertThat(it).isEqualTo(expectedIncidentNumbers)
            }
        }
      }
    }
  }

  @DisplayName("GET /incident-reports/{id}")
  @Nested
  inner class GetReportById {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri(url)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
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
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "USER1",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "USER1",
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
  inner class GetReportByIncidentNumber {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/incident-number/${existingReport.incidentNumber}"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri(url)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
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
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "USER1",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "USER1",
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
      incidentDateAndTime = LocalDateTime.now(clock).minusHours(1),
      title = "An incident occurred",
      description = "Longer explanation of incident",
      type = Type.SELF_HARM,
      prisonId = "MDI",
      reportedBy = "user2",
      reportedDate = LocalDateTime.now(clock),
      createNewEvent = true,
    )

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri(url)
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with right role, wrong scope`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }
    }

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
      }

      @Test
      fun `cannot create a report with an inactive type`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            jsonString(
              createReportRequest.copy(
                type = Type.OLD_ASSAULT,
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can add a new incident`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
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
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "INCIDENT_REPORTING_API"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "INCIDENT_REPORTING_API"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "user2",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "user2",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "INCIDENT_REPORTING_API",
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
          .bodyValue(jsonString(createReportRequest.copy(createNewEvent = false, linkedEventId = existingReport.event.eventId)))
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
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "INCIDENT_REPORTING_API"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "locations": [],
              "evidence": [],
              "correctionRequests": [],
              "reportedBy": "user2",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "user2",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "INCIDENT_REPORTING_API",
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
}
