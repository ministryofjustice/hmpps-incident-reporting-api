package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.CodeDescription
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Offender
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.OffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Staff
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.StaffParty
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDateTime

private const val INCIDENT_NUMBER: Long = 112414323

class MigrateResourceIntTest : SqsIntegrationTestBase() {

  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Autowired
  lateinit var repository: IncidentReportRepository

  lateinit var existingNomisIncident: IncidentReport

  @BeforeEach
  fun setUp() {
    repository.deleteAll()

    existingNomisIncident = repository.save(
      buildIncidentReport(incidentNumber = "$INCIDENT_NUMBER", reportTime = LocalDateTime.now(clock), source = InformationSource.NOMIS),
    )
  }

  @DisplayName("POST /sync/upsert")
  @Nested
  inner class MigrateIncidentReport {
    private val reportingStaff = Staff("user1", 121, "John", "Smith")
    val syncRequest = UpsertNomisIncident(
      initialMigration = false,
      incidentReport = NomisIncidentReport(
        incidentId = INCIDENT_NUMBER,
        title = "An incident occurred updated",
        description = "An incident occurred updated",
        prison = CodeDescription("MDI", "Moorland"),
        status = NomisIncidentStatus("AWAN", "Awaiting Analysis"),
        type = "SELF_HARM",
        lockedResponse = false,
        incidentDateTime = LocalDateTime.now(clock).minusHours(1),
        reportingStaff = reportingStaff,
        reportedDateTime = LocalDateTime.now(clock),
        staffParties = listOf(StaffParty(reportingStaff, CodeDescription("PRESENT", "Present at scene"), "REPORTER")),
        offenderParties = listOf(
          OffenderParty(
            offender = Offender(
              offenderNo = "A1234AA",
              firstName = "Trevor",
              lastName = "Smith",
            ),
            role = CodeDescription("PERP", "Perpetrator"),
            outcome = CodeDescription("XXXX", "XXXX"),
            comment = "Comment",
          ),
        ),
        requirements = listOf(),
        questions = listOf(),
        history = listOf(),
        questionnaireId = 1,
      ),
    )

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/sync/upsert")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf()))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(syncRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(syncRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with right role, wrong scope`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(syncRequest))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `access client error bad data`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue("""{ }""")
          .exchange()
          .expectStatus().is4xxClientError
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can migrate an incident`() {
        val now = LocalDateTime.now(clock)

        val updatedSyncRequest = syncRequest.copy(
          initialMigration = true,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = 112414666,
            description = "A New Incident From NOMIS",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(updatedSyncRequest))
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
          {
            "incidentType": "SELF_HARM",
            "incidentDateAndTime": "${updatedSyncRequest.incidentReport.incidentDateTime}",
            "prisonId": "${updatedSyncRequest.incidentReport.prison.code}",
            "incidentDetails": "${updatedSyncRequest.incidentReport.description}",
            "reportedBy": "user1",
            "reportedDate": "$now",
            "status": "${IncidentStatus.AWAITING_ANALYSIS.name}",
            "assignedTo": "user1",
            "createdDate": "$now",
            "lastModifiedDate": "$now",
            "lastModifiedBy": "user1",
            "createdInNomis": true
          }
          """,
            false,
          )
      }

      @Test
      fun `can sync an new incident after migration created in NOMIS`() {
        val now = LocalDateTime.now(clock)

        val newIncident = syncRequest.copy(
          initialMigration = false,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = INCIDENT_NUMBER + 1,
            description = "New NOMIS incident",
            type = "ASSAULTS3",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(newIncident))
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
           {
            "incidentNumber": "${newIncident.incidentReport.incidentId}",
            "incidentType": "${IncidentType.ASSAULT.name}",
            "incidentDateAndTime": "${newIncident.incidentReport.incidentDateTime}",
            "prisonId": "MDI",
            "incidentDetails": "New NOMIS incident",
            "reportedBy": "user1",
            "reportedDate": "$now",
            "status": "${IncidentStatus.AWAITING_ANALYSIS.name}",
            "assignedTo": "user1",
            "createdDate": "$now",
            "lastModifiedDate": "$now",
            "lastModifiedBy": "user1",
            "createdInNomis": true
          }
          """,
            false,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to message.additionalInformation?.source }).containsExactlyInAnyOrder(
            "incident.report.created" to InformationSource.NOMIS,
          )
        }
      }

      @Test
      fun `can sync an update to an existing incident created in NOMIS`() {
        val now = LocalDateTime.now(clock)

        val upsertMigration = syncRequest.copy(
          initialMigration = false,
          id = existingNomisIncident.id,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = INCIDENT_NUMBER,
            description = "Updated details",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(upsertMigration))
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
           {
            "id": "${existingNomisIncident.id}",
            "incidentNumber": "${existingNomisIncident.incidentNumber}",
            "incidentType": "${existingNomisIncident.incidentType}",
            "incidentDateAndTime": "${existingNomisIncident.incidentDateAndTime}",
            "prisonId": "MDI",
            "incidentDetails": "${upsertMigration.incidentReport.description}",
            "reportedBy": "USER1",
            "reportedDate": "$now",
            "status": "AWAITING_ANALYSIS",
            "assignedTo": "USER1",
            "createdDate": "$now",
            "lastModifiedDate": "$now",
            "lastModifiedBy": "user1",
            "createdInNomis": true
          }
          """,
            false,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to Pair(message.additionalInformation?.id, message.additionalInformation?.source) }).containsExactlyInAnyOrder(
            "incident.report.amended" to Pair(existingNomisIncident.id, InformationSource.NOMIS),
          )
        }
      }
    }
  }
}
