package uk.gov.justice.digital.hmpps.incidentreporting.resource

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
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentReportNumber
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
  lateinit var repository: IncidentReportRepository

  lateinit var existingIncident: IncidentReport

  @BeforeEach
  fun setUp() {
    repository.deleteAll()

    existingIncident = repository.save(
      buildIncidentReport(incidentNumber = repository.generateIncidentReportNumber(), reportTime = LocalDateTime.now(clock)),
    )
  }

  @DisplayName("POST /incident-reports")
  @Nested
  inner class CreateIncidentReport {

    val createIncidentReportRequest = CreateIncidentReportRequest(
      incidentDateAndTime = LocalDateTime.now(clock),
      incidentDetails = "An incident occurred",
      incidentType = IncidentType.SELF_HARM,
      prisonId = "MDI",
      reportedBy = "user1",
      reportedDate = LocalDateTime.now(clock),
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
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS"), scopes = listOf("read")))
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
      fun `can sync a new incident`() {
        val now = LocalDateTime.now(clock)
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
              "incidentDateAndTime": "${createIncidentReportRequest.incidentDateAndTime}",
              "prisonId": "MDI",
              "incidentDetails": "An incident occurred",
              "reportedBy": "user1",
              "reportedDate": "$now",
              "status": "DRAFT",
              "assignedTo": "user1",
              "createdDate": "$now",
              "lastModifiedDate": "$now",
              "lastModifiedBy": "INCIDENT_REPORTING_API",
              "createdInNomis": false
            }
          """,
            false,
          )
      }
    }
  }
}
