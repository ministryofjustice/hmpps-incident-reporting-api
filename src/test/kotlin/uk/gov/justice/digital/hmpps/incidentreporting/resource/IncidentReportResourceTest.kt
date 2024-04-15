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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CreateIncidentReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentEventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDateTime

class IncidentReportResourceTest : SqsIntegrationTestBase() {

  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Autowired
  lateinit var reportRepository: IncidentReportRepository

  @Autowired
  lateinit var eventRepository: IncidentEventRepository

  lateinit var existingIncident: IncidentReport

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    existingIncident = reportRepository.save(
      buildIncidentReport(
        incidentNumber = "IR-0000000001124143",
        reportTime = LocalDateTime.now(clock),
      ),
    )
  }

  @DisplayName("GET /incident-reports/{id}")
  @Nested
  inner class GetIncidentReport {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.get().uri("/incident-reports/${existingIncident.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incident-reports/${existingIncident.id}")
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incident-reports/${existingIncident.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with right role, wrong scope`() {
        webTestClient.get().uri("/incident-reports/${existingIncident.id}")
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
        webTestClient.get().uri("/incident-reports/${existingIncident.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingIncident.id}",
              "incidentNumber": "IR-0000000001124143",
              "incidentType": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "summary": "Incident Report IR-0000000001124143",
              "incidentDetails": "A new incident created in the new service of type Finds",
              "event": {
                "eventId": "IE-0000000001124143",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "An event occurred"
              },
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
        webTestClient.get().uri("/incident-reports/incident-number/${existingIncident.incidentNumber}")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingIncident.id}",
              "incidentNumber": "IR-0000000001124143",
              "incidentType": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "summary": "Incident Report IR-0000000001124143",
              "incidentDetails": "A new incident created in the new service of type Finds",
              "event": {
                "eventId": "IE-0000000001124143",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "An event occurred"
              },
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
  inner class CreateIncidentReport {

    val createIncidentReportRequest = CreateIncidentReportRequest(
      incidentDateAndTime = LocalDateTime.now(clock).minusHours(1),
      incidentDetails = "An incident occurred",
      incidentType = IncidentType.SELF_HARM,
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
          .bodyValue(jsonString(createIncidentReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createIncidentReportRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with right role, wrong scope`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createIncidentReportRequest))
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
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can add a new incident`() {
        webTestClient.post().uri("/incident-reports")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(createIncidentReportRequest))
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
            {
              "incidentType": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "incidentDetails": "An incident occurred",
              "event": {
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "An incident occurred"
              },
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
          .bodyValue(jsonString(createIncidentReportRequest.copy(createNewEvent = false, linkedEventId = existingIncident.event.eventId)))
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
            {
              "incidentType": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "incidentDetails": "An incident occurred",
              "event": {
                "eventId": "${existingIncident.event.eventId}",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "An event occurred"
              },
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
