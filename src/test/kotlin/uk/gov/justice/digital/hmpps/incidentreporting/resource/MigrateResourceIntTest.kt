package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.CodeDescription
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Offender
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.OffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Staff
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.StaffParty
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport as IncidentReportJPA

class MigrateResourceIntTest : SqsIntegrationTestBase() {

  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Autowired
  lateinit var repository: IncidentReportRepository
  lateinit var existingIncident: IncidentReportJPA

  @BeforeEach
  fun setUp() {
    repository.deleteAll()

    existingIncident = repository.save(
      IncidentReportJPA(
        incidentDateAndTime = LocalDateTime.now(clock).minusHours(1),
        incidentNumber = "1100011",
        incidentType = IncidentType.SELF_HARM,
        incidentDetails = "An self harm incident occurred",
        reportedBy = "user1",
        prisonId = "MDI",
        reportedDate = LocalDateTime.now(clock),
        assignedTo = "user2",
        createdDate = LocalDateTime.now(clock),
        lastModifiedDate = LocalDateTime.now(clock),
        lastModifiedBy = "user1",
      ),
    )
  }

  @DisplayName("POST /sync/upsert")
  @Nested
  inner class CreateLocationTest {
    private val reportingStaff = Staff("user1", 121, "John", "Smith")
    val syncRequest = UpsertNomisIncident(
      initialMigration = true,
      incidentReport = NomisIncidentReport(
        incidentId = 112414323,
        title = "An Incident occurred",
        description = "An incident occurred",
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
      fun `can sync a new incident`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(syncRequest))
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
             {
           
            }
          """,
            false,
          )
      }

      @Test
      fun `can sync an update to an existing incident`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(jsonString(syncRequest.copy(initialMigration = false, id = existingIncident.id, incidentReport = syncRequest.incidentReport.copy(description = "Updated details"))))
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
             {
             
            }
          """,
            false,
          )
      }
    }
  }
}
