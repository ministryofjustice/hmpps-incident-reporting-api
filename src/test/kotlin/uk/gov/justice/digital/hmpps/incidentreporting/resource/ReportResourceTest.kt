package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

  @DisplayName("GET /incident-reports/{id}")
  @Nested
  inner class GetReport {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/incident-reports/${existingReport.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incident-reports/${existingReport.id}")
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incident-reports/${existingReport.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with right role, wrong scope`() {
        webTestClient.get().uri("/incident-reports/${existingReport.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can get an incident by ID`() {
        webTestClient.get().uri("/incident-reports/${existingReport.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("write")))
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

      @Test
      fun `can get an incident by incident number`() {
        webTestClient.get().uri("/incident-reports/incident-number/${existingReport.incidentNumber}")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("write")))
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

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/incident-reports")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with right role, wrong scope`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `access client error bad data`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue("""{ }""")
          .exchange()
          .expectStatus().is4xxClientError
      }

      @Test
      fun `cannot create a report with an inactive type`() {
        webTestClient.post().uri("/incident-reports")
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

    @Nested
    inner class HappyPath {
      @Test
      fun `can add a new incident`() {
        webTestClient.post().uri("/incident-reports")
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
        webTestClient.post().uri("/incident-reports")
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
