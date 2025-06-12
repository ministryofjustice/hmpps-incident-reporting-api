package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.test.json.JsonAssert
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisCode
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistory
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistoryQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistoryResponse
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisOffender
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisOffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisRequirement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisResponse
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaff
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaffParty
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStatus
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.Clock
import java.util.UUID

/** NOMIS incident number maps to a report’s reference */
private const val NOMIS_INCIDENT_NUMBER: Long = 112414323

@DisplayName("NOMIS sync resource")
@Transactional
class NomisSyncResourceTest : SqsIntegrationTestBase() {

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

  lateinit var existingNomisReport: Report

  private fun deleteAllReports() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()
  }

  @BeforeEach
  fun setUp() {
    deleteAllReports()

    existingNomisReport = reportRepository.save(
      buildReport(
        reportReference = "$NOMIS_INCIDENT_NUMBER",
        reportTime = now,
        source = InformationSource.NOMIS,
        generateDescriptionAddendums = 2,
      ),
    )
  }

  @DisplayName("POST /sync/upsert")
  @Nested
  inner class MigrateReport {
    private val reportingStaff = NomisStaff("user2", 121, "John", "Smith")
    val syncRequest = NomisSyncRequest(
      initialMigration = false,
      incidentReport = NomisReport(
        incidentId = NOMIS_INCIDENT_NUMBER,
        title = "An incident occurred updated",
        description = "More details about the incident",
        prison = NomisCode("MDI", "Moorland"),
        status = NomisStatus("AWAN", "Awaiting Analysis"),
        type = "SELF_HARM",
        lockedResponse = false,
        incidentDateTime = now.minusHours(1),
        reportingStaff = reportingStaff,
        reportedDateTime = now,
        createDateTime = now.plusHours(2),
        createdBy = reportingStaff.username,
        lastModifiedDateTime = now.plusHours(5),
        lastModifiedBy = "another-user",
        staffParties = listOf(
          NomisStaffParty(
            staff = reportingStaff,
            sequence = 0,
            role = NomisCode("PAS", "Present at scene"),
            comment = "REPORTER",
            createDateTime = now,
            createdBy = reportingStaff.username,
          ),
        ),
        offenderParties = listOf(
          NomisOffenderParty(
            offender = NomisOffender(
              offenderNo = "A1234AA",
              firstName = "Trevor",
              lastName = "Smith",
            ),
            sequence = 0,
            role = NomisCode("PERP", "Perpetrator"),
            outcome = NomisCode("ACCT", "ACCT"),
            comment = "Comment",
            createDateTime = now,
            createdBy = reportingStaff.username,
          ),
        ),
        requirements = listOf(
          NomisRequirement(
            sequence = 0,
            comment = "Change 1",
            recordedDate = now,
            staff = reportingStaff,
            prisonId = "MDI",
            createDateTime = now,
            createdBy = reportingStaff.username,
          ),
          NomisRequirement(
            sequence = 1,
            comment = "Change 2",
            recordedDate = now.minusWeeks(1),
            staff = reportingStaff,
            prisonId = "MDI",
            createDateTime = now,
            createdBy = reportingStaff.username,
          ),
        ),
        questions = listOf(
          NomisQuestion(
            questionId = 4,
            sequence = 1,
            createDateTime = now,
            createdBy = reportingStaff.username,
            question = "Question 1",
            answers = listOf(
              NomisResponse(
                10,
                0,
                "Answer 1",
                today.minusDays(2),
                "comment 1",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 11,
                sequence = 1,
                answer = "Answer 2",
                responseDate = null,
                comment = "comment 2",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 12,
                sequence = 2,
                answer = "Answer 3",
                responseDate = today.minusDays(3),
                comment = null,
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
            ),
          ),
          NomisQuestion(
            questionId = 5,
            sequence = 2,
            createDateTime = now,
            createdBy = reportingStaff.username,
            question = "Question 2",
            answers = listOf(
              NomisResponse(
                questionResponseId = 13,
                sequence = 0,
                answer = "Answer 1",
                responseDate = today.minusDays(1),
                comment = "comment 1",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 14,
                sequence = 1,
                answer = "Answer 2",
                responseDate = null,
                comment = "comment 2",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 15,
                sequence = 2,
                answer = "Answer 3",
                responseDate = today.minusDays(10),
                comment = null,
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 16,
                sequence = 3,
                answer = "Answer 4",
                responseDate = null,
                comment = null,
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
            ),
          ),
          NomisQuestion(
            questionId = 6,
            sequence = 3,
            createDateTime = now,
            createdBy = reportingStaff.username,
            question = "Question 3",
            answers = listOf(
              NomisResponse(
                16,
                0,
                "Answer 1",
                today,
                "comment 1",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 17,
                sequence = 1,
                answer = "Answer 2",
                responseDate = null,
                comment = "comment 2",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                questionResponseId = 18,
                sequence = 2,
                answer = "Answer 3",
                responseDate = today.minusDays(7),
                comment = null,
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
            ),
          ),
        ),
        history = listOf(
          NomisHistory(
            questionnaireId = 1,
            type = "DAMAGE",
            description = "Damage",
            createDateTime = now,
            createdBy = reportingStaff.username,
            incidentChangeDateTime = now,
            incidentChangeStaff = reportingStaff,
            questions = listOf(
              NomisHistoryQuestion(
                questionId = 1,
                sequence = 1,
                question = "Old question 1",
                answers = listOf(
                  NomisHistoryResponse(1, 0, "Old answer 1", today, "comment 1", reportingStaff),
                  NomisHistoryResponse(2, 1, "Old answer 2", null, "comment 2", reportingStaff),
                  NomisHistoryResponse(3, 2, "Old answer 3", today.minusDays(7), null, reportingStaff),
                ),
              ),
              NomisHistoryQuestion(
                questionId = 2,
                sequence = 2,
                question = "Old question 2",
                answers = listOf(
                  NomisHistoryResponse(4, 0, "Old answer 1", today.minusDays(1), "comment 1", reportingStaff),
                  NomisHistoryResponse(5, 1, "Old answer 2", null, "comment 2", reportingStaff),
                  NomisHistoryResponse(6, 2, "Old answer 3", today.minusDays(8), null, reportingStaff),
                ),
              ),
              NomisHistoryQuestion(
                questionId = 3,
                sequence = 3,
                question = "Old question 3",
                answers = listOf(
                  NomisHistoryResponse(7, 0, "Old answer 1", null, null, reportingStaff),
                  NomisHistoryResponse(8, 1, "Old answer 2", null, null, reportingStaff),
                  NomisHistoryResponse(9, 2, "Old answer 3", null, null, reportingStaff),
                ),
              ),
            ),
          ),
        ),
        questionnaireId = 1,
      ),
    )

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.post().uri("/sync/upsert").bodyValue(syncRequest.toJson()),
        "MIGRATE_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `access client error bad data`() {
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue("""{ }""")
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `must use POST method`() {
        webTestClient.patch().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue("""{ }""")
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
      }

      @Test
      fun `returns 409 CONFLICT when report already created in initial migration`() {
        val initialSyncRequest = syncRequest.copy(
          initialMigration = true,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = 112414666,
            description = "A new incident from NOMIS",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(initialSyncRequest.toJson())
          .exchange()
          .expectStatus().isCreated

        val initialSyncRequestRetry = syncRequest.copy(
          initialMigration = true,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = 112414666,
            description = "A new incident from NOMIS sent again",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(initialSyncRequestRetry.toJson())
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.CONFLICT)
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Report already exists: 112414666")
          }
      }

      @Test
      fun `returns 409 CONFLICT when a report was modified in DPS since being created in NOMIS`() {
        // initial migration
        val initialMigrationRequest = syncRequest.copy(
          initialMigration = true,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = 112414666,
            description = "A new incident from NOMIS",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(initialMigrationRequest.toJson())
          .exchange()
          .expectStatus().isCreated

        // update report using DPS apis
        val reportId = reportRepository.findOneByReportReference("112414666")!!.id
        webTestClient.patch().uri("/incident-reports/$reportId")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
            {
              "description": "Updated in DPS"
            }
            """,
          )
          .exchange()
          .expectStatus().isOk

        // subsequent sync from a change in NOMIS
        val subsequentSyncRequest = syncRequest.copy(
          id = reportId,
          initialMigration = false,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = 112414666,
            description = "A new incident from NOMIS sent again",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(subsequentSyncRequest.toJson())
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.CONFLICT)
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Report last modified in DPS: $reportId")
          }
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can migrate an incident report`() {
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
          .bodyValue(updatedSyncRequest.toJson())
          .exchange()
          .expectStatus().isCreated
          .expectBody().jsonPath("id").value<String> {
            val reportId = UUID.fromString(it)
            val report = reportRepository.findOneEagerlyById(reportId)!!.toDtoWithDetails()
            val reportJson = report.toJson()
            JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(
              // language=json
              """
              {
                "reportReference": "112414666",
                "type": "SELF_HARM_1",
                "nomisType": "SELF_HARM",
                "incidentDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "A New Incident From NOMIS",
                "descriptionAddendums": [],
                "event": {
                  "eventReference": "112414666",
                  "eventDateAndTime": "2023-12-05T11:34:56",
                  "location": "MDI",
                  "prisonId": "MDI",
                  "title": "An incident occurred updated",
                  "description": "A New Incident From NOMIS",
                  "createdAt": "2023-12-05T14:34:56",
                  "modifiedAt": "2023-12-05T17:34:56",
                  "modifiedBy": "another-user"
                },
                "questions": [
                  {
                    "code": "4",
                    "question": "Question 1",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-03",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-12-02",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  },
                  {
                    "code": "5",
                    "question": "Question 2",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-04",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-11-25",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 4",
                        "responseDate": null,
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  },
                  {
                    "code": "6",
                    "question": "Question 3",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-05",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-11-28",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE_1",
                    "nomisType": "DAMAGE",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "1",
                        "question": "Old question 1",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": "2023-12-05",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": "2023-11-28",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      },
                      {
                        "code": "2",
                        "question": "Old question 2",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": "2023-12-04",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": "2023-11-27",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      },
                      {
                        "code": "3",
                        "question": "Old question 3",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      }
                    ]
                  }
                ],
                "historyOfStatuses": [
                  {
                    "status": "AWAITING_REVIEW",
                    "nomisStatus": "AWAN",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2"
                  }
                ],
                "staffInvolved": [
                  {
                    "staffUsername": "user2",
                    "firstName": "John",
                    "lastName": "Smith",
                    "staffRole": "PRESENT_AT_SCENE",
                    "comment": "REPORTER"
                  }
                ],
                "prisonersInvolved": [
                  {
                    "prisonerNumber": "A1234AA",
                    "firstName": "Trevor",
                    "lastName": "Smith",
                    "prisonerRole": "PERPETRATOR",
                    "outcome": "ACCT",
                    "comment": "Comment"
                  }
                ],
                "correctionRequests": [
                  {
                    "descriptionOfChange": "Change 1",
                    "location": "MDI",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-12-05T12:34:56"
                  },
                  {
                    "descriptionOfChange": "Change 2",
                    "location": "MDI",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-11-28T12:34:56"
                  }
                ],
                "staffInvolvementDone": true,
                "prisonerInvolvementDone": true,
                "reportedBy": "user2",
                "reportedAt": "2023-12-05T12:34:56",
                "status": "AWAITING_REVIEW",
                "nomisStatus": "AWAN",
                "assignedTo": "user2",
                "createdAt": "2023-12-05T14:34:56",
                "modifiedAt": "2023-12-05T17:34:56",
                "modifiedBy": "another-user",
                "createdInNomis": true,
                "lastModifiedInNomis": true
              }
              """,
              reportJson,
            )
          }

        assertThat(getNumberOfMessagesCurrentlyOnSubscriptionQueue()).isZero
      }

      @Test
      fun `can migrate an incident report with appended description`() {
        val updatedSyncRequest = syncRequest.copy(
          initialMigration = true,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = 112414666,
            description = "A New Incident From NOMIS" +
              "User:STARK,TONY Date:07-JUN-2024 12:13Some extra information" +
              "User:BANNER,BRUCE Date:10-JUN-2024 14:53Even more information",
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updatedSyncRequest.toJson())
          .exchange()
          .expectStatus().isCreated
          .expectBody().jsonPath("id").value<String> {
            val reportId = UUID.fromString(it)
            val report = reportRepository.findOneEagerlyById(reportId)!!.toDtoWithDetails()
            val reportJson = report.toJson()
            JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(
              // language=json
              """
              {
                "reportReference": "112414666",
                "type": "SELF_HARM_1",
                "nomisType": "SELF_HARM",
                "incidentDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "A New Incident From NOMIS",
                "descriptionAddendums": [
                  {
                    "createdBy": "INCIDENT_REPORTING_API",
                    "createdAt": "2024-06-07T12:13:00",
                    "firstName": "TONY",
                    "lastName": "STARK",
                    "text": "Some extra information"
                  },
                  {
                    "createdBy": "INCIDENT_REPORTING_API",
                    "createdAt": "2024-06-10T14:53:00",
                    "firstName": "BRUCE",
                    "lastName": "BANNER",
                    "text": "Even more information"
                  }
                ],
                "event": {
                  "eventReference": "112414666",
                  "eventDateAndTime": "2023-12-05T11:34:56",
                  "location": "MDI",
                  "prisonId": "MDI",
                  "title": "An incident occurred updated",
                  "description": "A New Incident From NOMISUser:STARK,TONY Date:07-JUN-2024 12:13Some extra informationUser:BANNER,BRUCE Date:10-JUN-2024 14:53Even more information",
                  "createdAt": "2023-12-05T14:34:56",
                  "modifiedAt": "2023-12-05T17:34:56",
                  "modifiedBy": "another-user"
                },
                "questions": [
                  {
                    "code": "4",
                    "question": "Question 1",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-03",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-12-02",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  },
                  {
                    "code": "5",
                    "question": "Question 2",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-04",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-11-25",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 4",
                        "responseDate": null,
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  },
                  {
                    "code": "6",
                    "question": "Question 3",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-05",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-11-28",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE_1",
                    "nomisType": "DAMAGE",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "1",
                        "question": "Old question 1",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": "2023-12-05",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": "2023-11-28",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      },
                      {
                        "code": "2",
                        "question": "Old question 2",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": "2023-12-04",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": "2023-11-27",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      },
                      {
                        "code": "3",
                        "question": "Old question 3",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      }
                    ]
                  }
                ],
                "historyOfStatuses": [
                  {
                    "status": "AWAITING_REVIEW",
                    "nomisStatus": "AWAN",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2"
                  }
                ],
                "staffInvolved": [
                  {
                    "staffUsername": "user2",
                    "firstName": "John",
                    "lastName": "Smith",
                    "staffRole": "PRESENT_AT_SCENE",
                    "comment": "REPORTER"
                  }
                ],
                "prisonersInvolved": [
                  {
                    "prisonerNumber": "A1234AA",
                    "firstName": "Trevor",
                    "lastName": "Smith",
                    "prisonerRole": "PERPETRATOR",
                    "outcome": "ACCT",
                    "comment": "Comment"
                  }
                ],
                "correctionRequests": [
                  {
                    "descriptionOfChange": "Change 1",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-12-05T12:34:56",
                    "location": "MDI"
                  },
                  {
                    "descriptionOfChange": "Change 2",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-11-28T12:34:56",
                    "location": "MDI"
                  }
                ],
                "staffInvolvementDone": true,
                "prisonerInvolvementDone": true,
                "reportedBy": "user2",
                "reportedAt": "2023-12-05T12:34:56",
                "status": "AWAITING_REVIEW",
                "nomisStatus": "AWAN",
                "assignedTo": "user2",
                "createdAt": "2023-12-05T14:34:56",
                "modifiedAt": "2023-12-05T17:34:56",
                "modifiedBy": "another-user",
                "createdInNomis": true,
                "lastModifiedInNomis": true
              }
              """,
              reportJson,
            )
          }

        assertThat(getNumberOfMessagesCurrentlyOnSubscriptionQueue()).isZero
      }

      @Test
      fun `can sync a new incident report after migration created in NOMIS`() {
        val newIncidentId = NOMIS_INCIDENT_NUMBER + 1
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
          .bodyValue(newIncident.toJson())
          .exchange()
          .expectStatus().isCreated
          .expectBody().jsonPath("id").value<String> {
            val reportId = UUID.fromString(it)
            val report = reportRepository.findOneEagerlyById(reportId)!!.toDtoWithDetails()
            val reportJson = report.toJson()
            JsonAssert.comparator(JsonCompareMode.LENIENT).assertIsMatch(
              // language=json
              """
              {
                "reportReference": "$newIncidentId",
                "type": "ASSAULT_5",
                "nomisType": "ASSAULTS3",
                "incidentDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "New NOMIS incident",
                "descriptionAddendums": [],
                "event": {
                  "eventReference": "$newIncidentId",
                  "eventDateAndTime": "2023-12-05T11:34:56",
                  "location": "MDI",
                  "prisonId": "MDI",
                  "title": "An incident occurred updated",
                  "description": "New NOMIS incident",
                  "createdAt": "2023-12-05T14:34:56",
                  "modifiedAt": "2023-12-05T17:34:56",
                  "modifiedBy": "another-user"
                },
                "questions": [
                  {
                    "code": "4",
                    "question": "Question 1",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-03",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-12-02",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  },
                  {
                    "code": "5",
                    "question": "Question 2",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-04",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-11-25",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 4",
                        "responseDate": null,
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  },
                  {
                    "code": "6",
                    "question": "Question 3",
                    "additionalInformation": null,
                    "responses": [
                      {
                        "response": "Answer 1",
                        "responseDate": "2023-12-05",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 2",
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      },
                      {
                        "response": "Answer 3",
                        "responseDate": "2023-11-28",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56"
                      }
                    ]
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE_1",
                    "nomisType": "DAMAGE",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "1",
                        "question": "Old question 1",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": "2023-12-05",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": "2023-11-28",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      },
                      {
                        "code": "2",
                        "question": "Old question 2",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": "2023-12-04",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": "2023-11-27",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      },
                      {
                        "code": "3",
                        "question": "Old question 3",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56"
                          }
                        ]
                      }
                    ]
                  }
                ],
                "historyOfStatuses": [
                  {
                    "status": "AWAITING_REVIEW",
                    "nomisStatus": "AWAN",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2"
                  }
                ],
                "staffInvolved": [
                  {
                    "staffUsername": "user2",
                    "firstName": "John",
                    "lastName": "Smith",
                    "staffRole": "PRESENT_AT_SCENE",
                    "comment": "REPORTER"
                  }
                ],
                "prisonersInvolved": [
                  {
                    "prisonerNumber": "A1234AA",
                    "firstName": "Trevor",
                    "lastName": "Smith",
                    "prisonerRole": "PERPETRATOR",
                    "outcome": "ACCT",
                    "comment": "Comment"
                  }
                ],
                "correctionRequests": [
                  {
                    "descriptionOfChange": "Change 1",
                    "location": "MDI",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-12-05T12:34:56"
                  },
                  {
                    "descriptionOfChange": "Change 2",
                    "location": "MDI",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-11-28T12:34:56"
                  }
                ],
                "staffInvolvementDone": true,
                "prisonerInvolvementDone": true,
                "reportedBy": "user2",
                "reportedAt": "2023-12-05T12:34:56",
                "status": "AWAITING_REVIEW",
                "nomisStatus": "AWAN",
                "assignedTo": "user2",
                "createdAt": "2023-12-05T14:34:56",
                "modifiedAt": "2023-12-05T17:34:56",
                "modifiedBy": "another-user",
                "createdInNomis": true,
                "lastModifiedInNomis": true
              }
              """,
              reportJson,
            )
          }
      }

      @Test
      fun `can sync an update to an existing incident report created in NOMIS`() {
        val upsertMigration = syncRequest.copy(
          initialMigration = false,
          id = existingNomisReport.id,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = NOMIS_INCIDENT_NUMBER,
            title = "Updated title",
            description = "Original description" +
              "User:Last 1,First 1 Date:05-DEC-2023 12:34Addendum #1" +
              "User:STARK,TONY Date:07-JUN-2024 12:13Some updated details",
            reportingStaff = NomisStaff("OF42", 42, "Oscar", "Foxtrot"),
            reportedDateTime = now.minusDays(1),
            createDateTime = now.minusDays(1),
            createdBy = "creator",
            lastModifiedDateTime = now.minusMinutes(5),
            lastModifiedBy = "updater",
            status = NomisStatus("INAN", "In Analysis"),
            questionnaireId = 419,
            type = "ASSAULTS3",
            incidentDateTime = now.minusDays(10),
            prison = NomisCode("FBI", "Forest Bank (HMP & YOI)"),
            staffParties = listOf(
              NomisStaffParty(
                sequence = 0,
                staff = NomisStaff("JAMESQ", 2, "James", "Quids"),
                role = NomisCode("PAS", "Present at scene"),
                comment = "James was also present actually",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
              NomisStaffParty(
                staff = reportingStaff,
                sequence = 1,
                role = NomisCode(code = "PAS", description = "Present at scene"),
                comment = "REPORTER",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
            ),
            offenderParties = listOf(
              NomisOffenderParty(
                offender = NomisOffender(
                  offenderNo = "A1234AA",
                  firstName = "Trevor",
                  lastName = "Smith",
                ),
                sequence = 0,
                role = NomisCode("PERP", "Perpetrator"),
                outcome = NomisCode("ILOC", "ILOC"),
                comment = "Trevor took another prisoner hostage",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
              NomisOffenderParty(
                offender = NomisOffender(
                  offenderNo = "B2222BB",
                  firstName = "John",
                  lastName = "Also-Smith",
                ),
                sequence = 1,
                role = NomisCode("HOST", "Hostage"),
                outcome = NomisCode("TRN", "Transfer"),
                comment = "Prisoner was transferred after incident",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
            ),
            requirements = listOf(
              NomisRequirement(
                sequence = 0,
                comment = "Could you update the title please",
                recordedDate = now.minusWeeks(1),
                staff = reportingStaff,
                prisonId = "MDI",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
              NomisRequirement(
                sequence = 1,
                comment = "Also the description",
                recordedDate = now,
                staff = reportingStaff,
                prisonId = "MDI",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
            ),
            questions = listOf(
              NomisQuestion(
                4,
                1,
                createDateTime = now,
                createdBy = reportingStaff.username,
                "Who was involved?",
                listOf(
                  NomisResponse(
                    10,
                    0,
                    "John",
                    today.minusDays(2),
                    "comment 1",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    11,
                    1,
                    "Trevor",
                    null,
                    "comment 2",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    12,
                    2,
                    "Maybe someone else?",
                    today.minusDays(3),
                    null,
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                ),
              ),
              NomisQuestion(
                5,
                2,
                createDateTime = now,
                createdBy = reportingStaff.username,
                "Where did this happen?",
                listOf(
                  NomisResponse(
                    13,
                    0,
                    "Cell",
                    today.minusDays(1),
                    "comment 1",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    14,
                    1,
                    "Landing",
                    null,
                    "comment 2",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    15,
                    2,
                    "Kitchen",
                    today.minusDays(10),
                    null,
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    16,
                    3,
                    "Exercise area",
                    null,
                    null,
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                ),
              ),
            ),
            history = listOf(
              NomisHistory(
                1,
                "DAMAGE",
                "Damage",
                incidentChangeDateTime = now.minusDays(2),
                incidentChangeStaff = reportingStaff,
                createDateTime = now.minusDays(2),
                createdBy = reportingStaff.username,
                questions = listOf(
                  NomisHistoryQuestion(
                    1,
                    1,
                    "Old question 1",
                    listOf(
                      NomisHistoryResponse(1, 0, "Old answer 1", today, "comment 1", reportingStaff),
                      NomisHistoryResponse(2, 1, "Old answer 2", null, "comment 2", reportingStaff),
                      NomisHistoryResponse(3, 2, "Old answer 3", today.minusDays(7), null, reportingStaff),
                    ),
                  ),
                  NomisHistoryQuestion(
                    2,
                    2,
                    "Old question 2",
                    listOf(
                      NomisHistoryResponse(4, 0, "Old answer 4", today.minusDays(1), "comment 1", reportingStaff),
                      NomisHistoryResponse(5, 1, "Old answer 5", null, "comment 2", reportingStaff),
                      NomisHistoryResponse(6, 2, "Old answer 6", today.minusDays(8), null, reportingStaff),
                    ),
                  ),
                ),
              ),
              NomisHistory(
                2,
                "BOMB",
                "Bomb",
                incidentChangeDateTime = now.minusDays(1),
                incidentChangeStaff = reportingStaff,
                createDateTime = now.minusDays(1),
                createdBy = reportingStaff.username,
                questions = listOf(
                  NomisHistoryQuestion(
                    11,
                    1,
                    "Old old question 1",
                    listOf(
                      NomisHistoryResponse(12, 0, "Old old answer 1", null, null, reportingStaff),
                      NomisHistoryResponse(22, 1, "Old old answer 2", null, null, reportingStaff),
                    ),
                  ),
                  NomisHistoryQuestion(
                    22,
                    2,
                    "Old old question 2",
                    listOf(
                      NomisHistoryResponse(44, 0, "Old old answer 1", null, null, reportingStaff),
                      NomisHistoryResponse(55, 1, "Old old answer 2", null, null, reportingStaff),
                    ),
                  ),
                ),
              ),
            ),
          ),
        )
        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(upsertMigration.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath("id").value<String> {
            val reportId = UUID.fromString(it)
            val report = reportRepository.findOneEagerlyById(reportId)!!.toDtoWithDetails()
            val reportJson = report.toJson()
            JsonAssert.comparator(JsonCompareMode.STRICT).assertIsMatch(
              // language=json
              """
              {
                "id": "${existingNomisReport.id}",
                "reportReference": "$NOMIS_INCIDENT_NUMBER",
                "type": "ASSAULT_5",
                "nomisType": "ASSAULTS3",
                "incidentDateAndTime": "2023-11-25T12:34:56",
                "location": "FBI",
                "prisonId": "FBI",
                "title": "Updated title",
                "description": "Original description",
                "descriptionAddendums": [
                  {
                    "sequence": 0,
                    "createdBy": "INCIDENT_REPORTING_API",
                    "createdAt": "2023-12-05T12:34:00",
                    "firstName": "First 1",
                    "lastName": "Last 1",
                    "text": "Addendum #1"
                  },
                  {
                    "sequence": 1,
                    "createdBy": "INCIDENT_REPORTING_API",
                    "createdAt": "2024-06-07T12:13:00",
                    "firstName": "TONY",
                    "lastName": "STARK",
                    "text": "Some updated details"
                  }
                ],
                "event": {
                  "id": "${existingNomisReport.event.id}",
                  "eventReference": "$NOMIS_INCIDENT_NUMBER",
                  "eventDateAndTime": "2023-11-25T12:34:56",
                  "location": "FBI",
                  "prisonId": "FBI",
                  "title": "Updated title",
                  "description": "Original description",
                  "createdAt": "2023-12-04T12:34:56",
                  "modifiedAt": "2023-12-05T12:29:56",
                  "modifiedBy": "updater"
                },
                "questions": [
                  {
                    "code": "4",
                    "question": "Who was involved?",
                    "sequence": 1,
                    "responses": [
                      {
                        "response": "John",
                        "sequence": 0,
                        "responseDate": "2023-12-03",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      },
                      {
                        "response": "Trevor",
                        "sequence": 1,
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      },
                      {
                        "response": "Maybe someone else?",
                        "sequence": 2,
                        "responseDate": "2023-12-02",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      }
                    ],
                    "additionalInformation": null
                  },
                  {
                    "code": "5",
                    "question": "Where did this happen?",
                    "sequence": 2,
                    "responses": [
                      {
                        "response": "Cell",
                        "sequence": 0,
                        "responseDate": "2023-12-04",
                        "additionalInformation": "comment 1",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      },
                      {
                        "response": "Landing",
                        "sequence": 1,
                        "responseDate": null,
                        "additionalInformation": "comment 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      },
                      {
                        "response": "Kitchen",
                        "sequence": 2,
                        "responseDate": "2023-11-25",
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      },
                      {
                        "response": "Exercise area",
                        "sequence": 3,
                        "responseDate": null,
                        "additionalInformation": null,
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56"
                      }
                    ],
                    "additionalInformation": null
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE_1",
                    "nomisType": "DAMAGE",
                    "changedAt": "2023-12-03T12:34:56",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "1",
                        "question": "Old question 1",
                        "sequence": 1,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "sequence": 0,
                            "responseDate": "2023-12-05",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          },
                          {
                            "response": "Old answer 2",
                            "sequence": 1,
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          },
                          {
                            "response": "Old answer 3",
                            "sequence": 2,
                            "responseDate": "2023-11-28",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          }
                        ],
                        "additionalInformation": null
                      },
                      {
                        "code": "2",
                        "question": "Old question 2",
                        "sequence": 2,
                        "responses": [
                          {
                            "response": "Old answer 4",
                            "sequence": 0,
                            "responseDate": "2023-12-04",
                            "additionalInformation": "comment 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          },
                          {
                            "response": "Old answer 5",
                            "sequence": 1,
                            "responseDate": null,
                            "additionalInformation": "comment 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          },
                          {
                            "response": "Old answer 6",
                            "sequence": 2,
                            "responseDate": "2023-11-27",
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          }
                        ],
                        "additionalInformation": null
                      }
                    ]
                  },
                  {
                    "type": "BOMB_1",
                    "nomisType": "BOMB",
                    "changedAt": "2023-12-04T12:34:56",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "11",
                        "question": "Old old question 1",
                        "sequence": 1,
                        "responses": [
                          {
                            "response": "Old old answer 1",
                            "sequence": 0,
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          },
                          {
                            "response": "Old old answer 2",
                            "sequence": 1,
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          }
                        ],
                        "additionalInformation": null
                      },
                      {
                        "code": "22",
                        "question": "Old old question 2",
                        "sequence": 2,
                        "responses": [
                          {
                            "response": "Old old answer 1",
                            "sequence": 0,
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          },
                          {
                            "response": "Old old answer 2",
                            "sequence": 1,
                            "responseDate": null,
                            "additionalInformation": null,
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56"
                          }
                        ],
                        "additionalInformation": null
                      }
                    ]
                  }
                ],
                "historyOfStatuses": [
                  {
                    "status": "DRAFT",
                    "nomisStatus": null,
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "USER1"
                  },
                  {
                    "status": "ON_HOLD",
                    "nomisStatus": "INAN",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "updater"
                  }
                ],
                "staffInvolved": [
                  {
                    "staffUsername": "JAMESQ",
                    "firstName": "James",
                    "lastName": "Quids",
                    "sequence": 0,
                    "staffRole": "PRESENT_AT_SCENE",
                    "comment": "James was also present actually"
                  },
                  {
                    "staffUsername": "user2",
                    "firstName": "John",
                    "lastName": "Smith",
                    "sequence": 1,
                    "staffRole": "PRESENT_AT_SCENE",
                    "comment": "REPORTER"
                  }
                ],
                "prisonersInvolved": [
                  {
                    "prisonerNumber": "A1234AA",
                    "firstName": "Trevor",
                    "lastName": "Smith",
                    "sequence": 0,
                    "prisonerRole": "PERPETRATOR",
                    "outcome": "LOCAL_INVESTIGATION",
                    "comment": "Trevor took another prisoner hostage"
                  },
                  {
                    "prisonerNumber": "B2222BB",
                    "firstName": "John",
                    "lastName": "Also-Smith",
                    "sequence": 1,
                    "prisonerRole": "HOSTAGE",
                    "outcome": "TRANSFER",
                    "comment": "Prisoner was transferred after incident"
                  }
                ],
                "correctionRequests": [
                  {
                    "sequence": 0,
                    "descriptionOfChange": "Could you update the title please",
                    "location": "MDI",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-11-28T12:34:56"
                  },
                  {
                    "sequence": 1,
                    "descriptionOfChange": "Also the description",
                    "location": "MDI",
                    "correctionRequestedBy": "user2",
                    "correctionRequestedAt": "2023-12-05T12:34:56"
                  }
                ],
                "staffInvolvementDone": true,
                "prisonerInvolvementDone": true,
                "reportedBy": "OF42",
                "reportedAt": "2023-12-04T12:34:56",
                "status": "ON_HOLD",
                "nomisStatus": "INAN",
                "assignedTo": "USER1",
                "createdAt": "2023-12-04T12:34:56",
                "modifiedAt": "2023-12-05T12:29:56",
                "modifiedBy": "updater",
                "createdInNomis": true,
                "lastModifiedInNomis": true
              }
              """,
              reportJson,
            )
          }
      }

      @Test
      fun `can update a question and response`() {
        val newReport = reportRepository.save(
          buildReport(
            reportReference = "${NOMIS_INCIDENT_NUMBER + 1}",
            reportTime = now,
            source = InformationSource.NOMIS,
            status = Status.CLOSED,
          ),
        )

        val q1 = newReport.addQuestion(code = "1", sequence = 1, question = "Q1")
        val q2 = newReport.addQuestion(code = "2", sequence = 2, question = "Q2")
        val q3 = newReport.addQuestion(code = "3", sequence = 3, question = "Q3")

        q1.addResponse(
          response = "Q1-R1",
          responseDate = today,
          sequence = 0,
          additionalInformation = "Info Q1-R1",
          recordedBy = "user1",
          recordedAt = now,
        )

        q1.addResponse(
          response = "Q1-R2",
          responseDate = today,
          sequence = 1,
          additionalInformation = "Info Q1-R2",
          recordedBy = "user2",
          recordedAt = now,
        )

        q2.addResponse(
          response = "Q2-R1",
          responseDate = today,
          sequence = 0,
          additionalInformation = "Info Q2",
          recordedBy = "user3",
          recordedAt = now,
        )

        q3.addResponse(
          response = "Q3-R1",
          responseDate = today,
          sequence = 0,
          additionalInformation = "Info Q3",
          recordedBy = "user4",
          recordedAt = now,
        )

        reportRepository.save(newReport)

        // These are the questions to replace
        val q1Replacement =
          NomisQuestion(
            questionId = 1,
            sequence = 1,
            createDateTime = now,
            createdBy = "user1",
            question = "Q1 Modified",
            answers = listOf(
              NomisResponse(
                questionResponseId = 100,
                sequence = 0,
                answer = "Q1-R1",
                responseDate = today,
                comment = "Info Q1-R1",
                recordingStaff = NomisStaff(username = "user1", staffId = 1L, firstName = "", lastName = ""),
                createDateTime = now,
                createdBy = "user1",
                lastModifiedDateTime = now,
                lastModifiedBy = "user1",
              ),
            ),
          )

        val q3Replacement =
          NomisQuestion(
            questionId = 3,
            sequence = 3,
            createDateTime = now,
            createdBy = "user5",
            question = "Q3 Modified",
            answers = listOf(
              NomisResponse(
                questionResponseId = 101,
                sequence = 0,
                answer = "Q3-R1 Mod",
                responseDate = today,
                comment = "Info Q3 Changed",
                recordingStaff = NomisStaff(username = "user5", staffId = 1L, firstName = "", lastName = ""),
                createDateTime = now,
                createdBy = "user5",
                lastModifiedDateTime = now,
                lastModifiedBy = "user5",
              ),
              NomisResponse(
                questionResponseId = 102,
                sequence = 1,
                answer = "Q3-R2 New",
                responseDate = today,
                comment = "Info Q3 Added response",
                recordingStaff = NomisStaff(username = "user5", staffId = 1L, firstName = "", lastName = ""),
                createDateTime = now,
                createdBy = "user5",
                lastModifiedDateTime = now,
                lastModifiedBy = "user5",
              ),
            ),
          )

        val newQuestion =
          NomisQuestion(
            questionId = 4,
            sequence = 4,
            createDateTime = now,
            createdBy = "user6",
            question = "Q4 New",
            answers = listOf(
              NomisResponse(
                questionResponseId = 102,
                sequence = 0,
                answer = "Q4",
                responseDate = today,
                comment = "Info Q4",
                recordingStaff = NomisStaff(username = "user6", staffId = 1L, firstName = "", lastName = ""),
                createDateTime = now,
                createdBy = "user6",
                lastModifiedDateTime = now,
                lastModifiedBy = "user6",
              ),
            ),
          )

        val syncRequest = NomisSyncRequest(
          id = newReport.id,
          initialMigration = false,
          incidentReport = NomisReport(
            description = newReport.description,
            incidentId = 1L,
            questionnaireId = 1L,
            title = newReport.title,
            prison = NomisCode(code = newReport.location, description = ""),
            status = NomisStatus(code = newReport.status.nomisStatus!!, description = ""),
            type = newReport.type.nomisType!!,
            lockedResponse = false,
            incidentDateTime = newReport.incidentDateAndTime,
            reportingStaff = NomisStaff(newReport.reportedBy, staffId = 1L, firstName = "", lastName = ""),
            reportedDateTime = newReport.reportedAt,
            createDateTime = newReport.createdAt,
            createdBy = newReport.modifiedBy,
            lastModifiedDateTime = newReport.modifiedAt,
            lastModifiedBy = newReport.modifiedBy,
            followUpDate = null,
            staffParties = listOf(),
            offenderParties = listOf(),
            requirements = listOf(),
            questions = listOf(q1Replacement, q3Replacement, newQuestion),
            history = listOf(),
          ),
        )

        webTestClient.post().uri("/sync/upsert")
          .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(syncRequest.toJson())
          .exchange()
          .expectStatus().isOk

        val changedReport =
          reportRepository.findOneEagerlyById(newReport.id!!) ?: throw RuntimeException("Report not found")
        assertThat(changedReport.questions).hasSize(3)
        assertThat(changedReport.findQuestion(code = "1", sequence = 1)).isNotNull
        assertThat(changedReport.findQuestion(code = "2", sequence = 2)).isNull()
        assertThat(changedReport.findQuestion(code = "3", sequence = 3)).isNotNull
        assertThat(changedReport.findQuestion(code = "4", sequence = 4)).isNotNull
      }
    }
  }

  @DisplayName("POST /sync/upsert")
  @Nested
  inner class SamplePayloads {
    private val nomisReportPayload = getResource("/nomis-sync/sample-report.json")

    private fun sendAuthorisedSyncRequest(
      initialMigration: Boolean,
      incidentIdToUpdate: UUID?,
      assertions: WebTestClient.ResponseSpec.() -> Unit,
    ) {
      val quotedIncidentId = if (incidentIdToUpdate == null) "null" else "\"$incidentIdToUpdate\""
      val body = """{
        "id": $quotedIncidentId,
        "initialMigration": $initialMigration,
        "incidentReport": $nomisReportPayload
      }"""
      webTestClient.post().uri("/sync/upsert")
        .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_INCIDENT_REPORTS"), scopes = listOf("write")))
        .header("Content-Type", "application/json")
        .bodyValue(body)
        .exchange()
        .apply(assertions)
    }

    @Test
    fun `can create a report during initial migration`() {
      deleteAllReports() // drop reports from test setup to prevent report and event reference clashes

      sendAuthorisedSyncRequest(
        initialMigration = true,
        incidentIdToUpdate = null,
      ) {
        // new report created
        expectStatus().isCreated
      }
    }

    @Test
    fun `can create a report after initial migration`() {
      deleteAllReports() // drop reports from test setup to prevent report and event reference clashes

      sendAuthorisedSyncRequest(
        initialMigration = false,
        incidentIdToUpdate = null,
      ) {
        // new report created
        expectStatus().isCreated
      }
    }

    @Test
    fun `cannot update a report during initial migration`() {
      sendAuthorisedSyncRequest(
        initialMigration = true,
        incidentIdToUpdate = existingNomisReport.id,
      ) {
        // invalid options
        expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains(
              "Cannot update an existing report (${existingNomisReport.id}) during initial migration",
            )
          }
      }
    }

    @Test
    fun `can update a report after initial migration`() {
      sendAuthorisedSyncRequest(
        initialMigration = false,
        incidentIdToUpdate = existingNomisReport.id,
      ) {
        // existing report updated
        expectStatus().isOk
      }
    }
  }
}
