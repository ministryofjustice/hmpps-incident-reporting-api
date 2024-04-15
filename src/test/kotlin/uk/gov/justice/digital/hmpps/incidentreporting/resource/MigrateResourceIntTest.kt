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
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentEventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.CodeDescription
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.History
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.HistoryQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.HistoryResponse
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Offender
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.OffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Question
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Requirement
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Response
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.Staff
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.StaffParty
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.time.Clock
import java.time.LocalDate
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
  lateinit var reportRepository: IncidentReportRepository

  @Autowired
  lateinit var eventRepository: IncidentEventRepository

  lateinit var existingNomisIncident: IncidentReport

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    existingNomisIncident = reportRepository.save(
      buildIncidentReport(
        incidentNumber = "$INCIDENT_NUMBER",
        reportTime = LocalDateTime.now(clock),
        source = InformationSource.NOMIS,
      ),
    )
  }

  @DisplayName("POST /sync/upsert")
  @Nested
  inner class MigrateIncidentReport {
    private val reportingStaff = Staff("user2", 121, "John", "Smith")
    val syncRequest = UpsertNomisIncident(
      initialMigration = false,
      incidentReport = NomisIncidentReport(
        incidentId = INCIDENT_NUMBER,
        title = "An incident occurred updated",
        description = "More details about the incident",
        prison = CodeDescription("MDI", "Moorland"),
        status = NomisIncidentStatus("AWAN", "Awaiting Analysis"),
        type = "SELF_HARM",
        lockedResponse = false,
        incidentDateTime = LocalDateTime.now(clock).minusHours(1),
        reportingStaff = reportingStaff,
        reportedDateTime = LocalDateTime.now(clock),
        staffParties = listOf(StaffParty(reportingStaff, CodeDescription("PAS", "Present at scene"), "REPORTER")),
        offenderParties = listOf(
          OffenderParty(
            offender = Offender(
              offenderNo = "A1234AA",
              firstName = "Trevor",
              lastName = "Smith",
            ),
            role = CodeDescription("PERP", "Perpetrator"),
            outcome = CodeDescription("ACCT", "ACCT"),
            comment = "Comment",
          ),
        ),
        requirements = listOf(
          Requirement("Change 1", LocalDate.now(clock), reportingStaff, "MDI"),
          Requirement("Change 2", LocalDate.now(clock).minusWeeks(1), reportingStaff, "MDI"),
        ),
        questions = listOf(
          Question(
            1,
            1,
            "Question 1",
            listOf(
              Response(1, 1, "Answer 1", "comment 1", reportingStaff),
              Response(1, 2, "Answer 2", "comment 2", reportingStaff),
              Response(1, 3, "Answer 3", "comment 3", reportingStaff),
            ),
          ),
          Question(
            2,
            2,
            "Question 2",
            listOf(
              Response(2, 1, "Answer 1", "comment 1", reportingStaff),
              Response(2, 2, "Answer 2", "comment 2", reportingStaff),
              Response(2, 3, "Answer 3", "comment 3", reportingStaff),
            ),
          ),
          Question(
            3,
            3,
            "Question 3",
            listOf(
              Response(3, 1, "Answer 1", "comment 1", reportingStaff),
              Response(3, 2, "Answer 2", "comment 2", reportingStaff),
              Response(3, 3, "Answer 3", "comment 3", reportingStaff),
            ),
          ),
        ),
        history = listOf(
          History(
            1,
            "DAMAGE",
            "Damage",
            incidentChangeDate = LocalDate.now(),
            incidentChangeStaff = reportingStaff,
            questions = listOf(
              HistoryQuestion(
                1,
                1,
                "Question 1",
                listOf(
                  HistoryResponse(1, 1, "Answer 1", "comment 1", reportingStaff),
                  HistoryResponse(1, 2, "Answer 2", "comment 2", reportingStaff),
                  HistoryResponse(1, 3, "Answer 3", "comment 3", reportingStaff),
                ),
              ),
              HistoryQuestion(
                2,
                2,
                "Question 2",
                listOf(
                  HistoryResponse(2, 1, "Answer 1", "comment 1", reportingStaff),
                  HistoryResponse(2, 2, "Answer 2", "comment 2", reportingStaff),
                  HistoryResponse(2, 3, "Answer 3", "comment 3", reportingStaff),
                ),
              ),
              HistoryQuestion(
                3,
                3,
                "Question 3",
                listOf(
                  HistoryResponse(3, 1, "Answer 1", "comment 1", reportingStaff),
                  HistoryResponse(3, 2, "Answer 2", "comment 2", reportingStaff),
                  HistoryResponse(3, 3, "Answer 3", "comment 3", reportingStaff),
                ),
              ),
            ),
          ),
        ),
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
              "incidentNumber": "112414666",
              "incidentType": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "summary": "An incident occurred updated",
              "incidentDetails": "A New Incident From NOMIS",
              "event": {
                "eventId": "112414666",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "A New Incident From NOMIS"
              },
              "reportedBy": "user2",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "user2",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "user2",
              "createdInNomis": true
            }
            """,
            false,
          )
      }

      @Test
      fun `can sync an new incident after migration created in NOMIS`() {
        val newIncidentId = INCIDENT_NUMBER + 1
        val newIncident = syncRequest.copy(
          initialMigration = false,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = newIncidentId,
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
              "incidentNumber": "$newIncidentId",
              "incidentType": "ASSAULT",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "summary": "An incident occurred updated",
              "incidentDetails": "New NOMIS incident",
              "event": {
                "eventId": "$newIncidentId",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "New NOMIS incident"
              },
              "reportedBy": "user2",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "user2",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "user2",
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
              "incidentNumber": "$INCIDENT_NUMBER",
              "incidentType": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "summary": "An incident occurred updated",
              "incidentDetails": "Updated details",
              "event": {
                "eventId": "$INCIDENT_NUMBER",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "eventDetails": "An event occurred"
              },
              "reportedBy": "USER1",
              "reportedDate": "2023-12-05T12:34:56",
              "status": "AWAITING_ANALYSIS",
              "assignedTo": "USER1",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "user2",
              "createdInNomis": true
            }
            """,
            true,
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
