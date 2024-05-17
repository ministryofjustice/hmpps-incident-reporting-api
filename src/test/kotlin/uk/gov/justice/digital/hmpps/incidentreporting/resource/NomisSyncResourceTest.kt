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
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
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
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private const val INCIDENT_NUMBER: Long = 112414323

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
      buildIncidentReport(
        incidentNumber = "$INCIDENT_NUMBER",
        reportTime = LocalDateTime.now(clock),
        source = InformationSource.NOMIS,
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
        incidentId = INCIDENT_NUMBER,
        title = "An incident occurred updated",
        description = "More details about the incident",
        prison = NomisCode("MDI", "Moorland"),
        status = NomisStatus("AWAN", "Awaiting Analysis"),
        type = "SELF_HARM",
        lockedResponse = false,
        incidentDateTime = LocalDateTime.now(clock).minusHours(1),
        reportingStaff = reportingStaff,
        reportedDateTime = LocalDateTime.now(clock),
        staffParties = listOf(
          NomisStaffParty(reportingStaff, NomisCode("PAS", "Present at scene"), "REPORTER"),
        ),
        offenderParties = listOf(
          NomisOffenderParty(
            offender = NomisOffender(
              offenderNo = "A1234AA",
              firstName = "Trevor",
              lastName = "Smith",
            ),
            role = NomisCode("PERP", "Perpetrator"),
            outcome = NomisCode("ACCT", "ACCT"),
            comment = "Comment",
          ),
        ),
        requirements = listOf(
          NomisRequirement("Change 1", LocalDate.now(clock), reportingStaff, "MDI"),
          NomisRequirement("Change 2", LocalDate.now(clock).minusWeeks(1), reportingStaff, "MDI"),
        ),
        questions = listOf(
          NomisQuestion(
            4,
            1,
            "Question 1",
            listOf(
              NomisResponse(10, 1, "Answer 1", "comment 1", reportingStaff),
              NomisResponse(11, 2, "Answer 2", "comment 2", reportingStaff),
              NomisResponse(12, 3, "Answer 3", "comment 3", reportingStaff),
            ),
          ),
          NomisQuestion(
            5,
            2,
            "Question 2",
            listOf(
              NomisResponse(13, 1, "Answer 1", "comment 1", reportingStaff),
              NomisResponse(14, 2, "Answer 2", "comment 2", reportingStaff),
              NomisResponse(15, 3, "Answer 3", "comment 3", reportingStaff),
            ),
          ),
          NomisQuestion(
            6,
            3,
            "Question 3",
            listOf(
              NomisResponse(16, 1, "Answer 1", "comment 1", reportingStaff),
              NomisResponse(17, 2, "Answer 2", "comment 2", reportingStaff),
              NomisResponse(18, 3, "Answer 3", "comment 3", reportingStaff),
            ),
          ),
        ),
        history = listOf(
          NomisHistory(
            1,
            "DAMAGE",
            "Damage",
            incidentChangeDate = LocalDate.now(clock),
            incidentChangeStaff = reportingStaff,
            questions = listOf(
              NomisHistoryQuestion(
                1,
                1,
                "Old question 1",
                listOf(
                  NomisHistoryResponse(1, 1, "Old answer 1", "comment 1", reportingStaff),
                  NomisHistoryResponse(2, 2, "Old answer 2", "comment 2", reportingStaff),
                  NomisHistoryResponse(3, 3, "Old answer 3", "comment 3", reportingStaff),
                ),
              ),
              NomisHistoryQuestion(
                2,
                2,
                "Old question 2",
                listOf(
                  NomisHistoryResponse(4, 1, "Old answer 1", "comment 1", reportingStaff),
                  NomisHistoryResponse(5, 2, "Old answer 2", "comment 2", reportingStaff),
                  NomisHistoryResponse(6, 3, "Old answer 3", "comment 3", reportingStaff),
                ),
              ),
              NomisHistoryQuestion(
                3,
                3,
                "Old question 3",
                listOf(
                  NomisHistoryResponse(7, 1, "Old answer 1", "comment 1", reportingStaff),
                  NomisHistoryResponse(8, 2, "Old answer 2", "comment 2", reportingStaff),
                  NomisHistoryResponse(9, 3, "Old answer 3", "comment 3", reportingStaff),
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
    }

    @DisplayName("works")
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
              "type": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An incident occurred updated",
              "description": "A New Incident From NOMIS",
              "event": {
                "eventId": "112414666",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "A New Incident From NOMIS",
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "user2"
              },
              "questions": [
                {
                  "code": "QID-000000000004",
                  "question": "Question 1",
                  "additionalInformation": null,
                  "responses": [
                    {
                      "response": "Answer 1",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Answer 2",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Answer 3",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ]
                },
                {
                  "code": "QID-000000000005",
                  "question": "Question 2",
                  "additionalInformation": null,
                  "responses": [
                    {
                      "response": "Answer 1",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Answer 2",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Answer 3",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ]
                },
                {
                  "code": "QID-000000000006",
                  "question": "Question 3",
                  "additionalInformation": null,
                  "responses": [
                    {
                      "response": "Answer 1",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Answer 2",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Answer 3",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ]
                }
              ],
              "history": [
                {
                  "type": "DAMAGE",
                  "changeDate": "2023-12-05T00:00:00",
                  "changeStaffUsername": "user2",
                  "questions": [
                    {
                      "code": "QID-000000000001",
                      "question": "Old question 1",
                      "additionalInformation": null,
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 2"
                        },
                        {
                          "response": "Old answer 3",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 3"
                        }
                      ]
                    },
                    {
                      "code": "QID-000000000002",
                      "question": "Old question 2",
                      "additionalInformation": null,
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 2"
                        },
                        {
                          "response": "Old answer 3",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 3"
                        }
                      ]
                    },
                    {
                      "code": "QID-000000000003",
                      "question": "Old question 3",
                      "additionalInformation": null,
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 2"
                        },
                        {
                          "response": "Old answer 3",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 3"
                        }
                      ]
                    }
                  ]
                }
              ],
              "historyOfStatuses": [
                {
                  "status": "AWAITING_ANALYSIS",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "user2"
                }
              ],
              "staffInvolved": [
                {
                  "staffUsername": "user2",
                  "staffRole": "PRESENT_AT_SCENE",
                  "comment": "REPORTER"
                }
              ],
              "prisonersInvolved": [
                {
                  "prisonerNumber": "A1234AA",
                  "prisonerRole": "PERPETRATOR",
                  "outcome": "ACCT",
                  "comment": "Comment"
                }
              ],
              "locations": [],
              "evidence": [],
              "correctionRequests": [
                {
                  "reason": "NOT_SPECIFIED",
                  "descriptionOfChange": "Change 1",
                  "correctionRequestedBy": "user2",
                  "correctionRequestedAt": "2023-12-05T00:00:00"
                },
                {
                  "reason": "NOT_SPECIFIED",
                  "descriptionOfChange": "Change 2",
                  "correctionRequestedBy": "user2",
                  "correctionRequestedAt": "2023-11-28T00:00:00"
                }
              ],
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

        assertThat(getNumberOfMessagesCurrentlyOnSubscriptionQueue()).isZero
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
              "type": "ASSAULT",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An incident occurred updated",
              "description": "New NOMIS incident",
              "event": {
                "eventId": "$newIncidentId",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "New NOMIS incident",
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "user2"
              },
              "questions": [
                {
                  "code": "QID-000000000004",
                  "question": "Question 1",
                  "additionalInformation": null,
                  "responses": [
                    {
                      "response": "Answer 1",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Answer 2",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Answer 3",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ]
                },
                {
                  "code": "QID-000000000005",
                  "question": "Question 2",
                  "additionalInformation": null,
                  "responses": [
                    {
                      "response": "Answer 1",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Answer 2",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Answer 3",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ]
                },
                {
                  "code": "QID-000000000006",
                  "question": "Question 3",
                  "additionalInformation": null,
                  "responses": [
                    {
                      "response": "Answer 1",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Answer 2",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Answer 3",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-05T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ]
                }
              ],
              "history": [
                {
                  "type": "DAMAGE",
                  "changeDate": "2023-12-05T00:00:00",
                  "changeStaffUsername": "user2",
                  "questions": [
                    {
                      "code": "QID-000000000001",
                      "question": "Old question 1",
                      "additionalInformation": null,
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 2"
                        },
                        {
                          "response": "Old answer 3",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 3"
                        }
                      ]
                    },
                    {
                      "code": "QID-000000000002",
                      "question": "Old question 2",
                      "additionalInformation": null,
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 2"
                        },
                        {
                          "response": "Old answer 3",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 3"
                        }
                      ]
                    },
                    {
                      "code": "QID-000000000003",
                      "question": "Old question 3",
                      "additionalInformation": null,
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 2"
                        },
                        {
                          "response": "Old answer 3",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-05T12:34:56",
                          "additionalInformation": "comment 3"
                        }
                      ]
                    }
                  ]
                }
              ],
              "historyOfStatuses": [
                {
                  "status": "AWAITING_ANALYSIS",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "user2"
                }
              ],
              "staffInvolved": [
                {
                  "staffUsername": "user2",
                  "staffRole": "PRESENT_AT_SCENE",
                  "comment": "REPORTER"
                }
              ],
              "prisonersInvolved": [
                {
                  "prisonerNumber": "A1234AA",
                  "prisonerRole": "PERPETRATOR",
                  "outcome": "ACCT",
                  "comment": "Comment"
                }
              ],
              "locations": [],
              "evidence": [],
              "correctionRequests": [
                {
                  "reason": "NOT_SPECIFIED",
                  "descriptionOfChange": "Change 1",
                  "correctionRequestedBy": "user2",
                  "correctionRequestedAt": "2023-12-05T00:00:00"
                },
                {
                  "reason": "NOT_SPECIFIED",
                  "descriptionOfChange": "Change 2",
                  "correctionRequestedBy": "user2",
                  "correctionRequestedAt": "2023-11-28T00:00:00"
                }
              ],
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
          id = existingNomisReport.id,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = INCIDENT_NUMBER,
            title = "Updated title",
            description = "Updated details",
            reportingStaff = NomisStaff("OF42", 42, "Oscar", "Foxtrot"),
            reportedDateTime = LocalDateTime.now(clock).minusDays(1),
            status = NomisStatus("INAN", "In Analysis"),
            questionnaireId = 419,
            type = "ASSAULTS3",
            incidentDateTime = LocalDateTime.now(clock).minusDays(10),
            prison = NomisCode("FBI", "Forest Bank (HMP & YOI)"),
            staffParties = listOf(
              NomisStaffParty(reportingStaff, NomisCode("PAS", "Present at scene"), "REPORTER"),
              NomisStaffParty(
                NomisStaff("JAMESQ", 2, "James", "Quids"),
                NomisCode("PAS", "Present at scene"),
                "James was also present actually",
              ),
            ),
            offenderParties = listOf(
              NomisOffenderParty(
                offender = NomisOffender(
                  offenderNo = "B2222BB",
                  firstName = "John",
                  lastName = "Also-Smith",
                ),
                role = NomisCode("HOST", "Hostage"),
                outcome = NomisCode("TRN", "Transfer"),
                comment = "Prisoner was transferred after incident",
              ),
              NomisOffenderParty(
                offender = NomisOffender(
                  offenderNo = "A1234AA",
                  firstName = "Trevor",
                  lastName = "Smith",
                ),
                role = NomisCode("PERP", "Perpetrator"),
                outcome = NomisCode("ILOC", "ILOC"),
                comment = "Trevor took another prisoner hostage",
              ),
            ),
            requirements = listOf(
              NomisRequirement("Also the description", LocalDate.now(clock), reportingStaff, "MDI"),
              NomisRequirement("Could you update the title please", LocalDate.now(clock).minusWeeks(1), reportingStaff, "MDI"),
            ),
            questions = listOf(
              NomisQuestion(
                4,
                1,
                "Who was involved?",
                listOf(
                  NomisResponse(10, 1, "John", "comment 1", reportingStaff),
                  NomisResponse(11, 2, "Trevor", "comment 2", reportingStaff),
                  NomisResponse(12, 3, "Maybe someone else?", "comment 3", reportingStaff),
                ),
              ),
              NomisQuestion(
                5,
                2,
                "Where did this happen?",
                listOf(
                  NomisResponse(13, 1, "Cell", "comment 1", reportingStaff),
                  NomisResponse(14, 2, "Landing", "comment 2", reportingStaff),
                  NomisResponse(15, 3, "Kitchen", "comment 3", reportingStaff),
                ),
              ),
            ),
            history = listOf(
              NomisHistory(
                1,
                "DAMAGE",
                "Damage",
                incidentChangeDate = LocalDate.now(clock),
                incidentChangeStaff = reportingStaff,
                questions = listOf(
                  NomisHistoryQuestion(
                    1,
                    1,
                    "Old question 1",
                    listOf(
                      NomisHistoryResponse(1, 1, "Old answer 1", "comment 1", reportingStaff),
                      NomisHistoryResponse(2, 2, "Old answer 2", "comment 2", reportingStaff),
                    ),
                  ),
                  NomisHistoryQuestion(
                    2,
                    2,
                    "Old question 2",
                    listOf(
                      NomisHistoryResponse(4, 1, "Old answer 1", "comment 1", reportingStaff),
                      NomisHistoryResponse(5, 2, "Old answer 2", "comment 2", reportingStaff),
                    ),
                  ),
                ),
              ),
              NomisHistory(
                2,
                "BOMB",
                "Bomb",
                incidentChangeDate = LocalDate.now(clock).minusDays(2),
                incidentChangeStaff = reportingStaff,
                questions = listOf(
                  NomisHistoryQuestion(
                    11,
                    1,
                    "Old old question 1",
                    listOf(
                      NomisHistoryResponse(12, 1, "Old old answer 1", "comment 1", reportingStaff),
                      NomisHistoryResponse(22, 2, "Old old answer 2", "comment 2", reportingStaff),
                    ),
                  ),
                  NomisHistoryQuestion(
                    22,
                    2,
                    "Old old question 2",
                    listOf(
                      NomisHistoryResponse(44, 1, "Old old answer 1", "comment 1", reportingStaff),
                      NomisHistoryResponse(55, 2, "Old old answer 2", "comment 2", reportingStaff),
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
          .bodyValue(jsonString(upsertMigration))
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
             {
              "id": "${existingNomisReport.id}",
              "incidentNumber": "$INCIDENT_NUMBER",
              "type": "ASSAULT",
              "incidentDateAndTime": "2023-11-25T12:34:56",
              "prisonId": "FBI",
              "title": "Updated title",
              "description": "Updated details",
              "event": {
                "eventId": "$INCIDENT_NUMBER",
                "eventDateAndTime": "2023-11-25T12:34:56",
                "prisonId": "FBI",
                "title": "Updated title",
                "description": "Updated details",
                "createdDate": "2023-12-05T12:34:56",
                "lastModifiedDate": "2023-12-05T12:34:56",
                "lastModifiedBy": "OF42"
              },
              "questions": [
                {
                  "code": "QID-000000000004",
                  "question": "Who was involved?",
                  "responses": [
                    {
                      "response": "John",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-04T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Trevor",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-04T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Maybe someone else?",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-04T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ],
                  "additionalInformation": null
                },
                {
                  "code": "QID-000000000005",
                  "question": "Where did this happen?",
                  "responses": [
                    {
                      "response": "Cell",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-04T12:34:56",
                      "additionalInformation": "comment 1"
                    },
                    {
                      "response": "Landing",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-04T12:34:56",
                      "additionalInformation": "comment 2"
                    },
                    {
                      "response": "Kitchen",
                      "recordedBy": "user2",
                      "recordedOn": "2023-12-04T12:34:56",
                      "additionalInformation": "comment 3"
                    }
                  ],
                  "additionalInformation": null
                }
              ],
              "history": [
                {
                  "type": "DAMAGE",
                  "changeDate": "2023-12-05T00:00:00",
                  "changeStaffUsername": "user2",
                  "questions": [
                    {
                      "code": "QID-000000000001",
                      "question": "Old question 1",
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 2"
                        }
                      ],
                      "additionalInformation": null
                    },
                    {
                      "code": "QID-000000000002",
                      "question": "Old question 2",
                      "responses": [
                        {
                          "response": "Old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 2"
                        }
                      ],
                      "additionalInformation": null
                    }
                  ]
                },
                {
                  "type": "BOMB_THREAT",
                  "changeDate": "2023-12-03T00:00:00",
                  "changeStaffUsername": "user2",
                  "questions": [
                    {
                      "code": "QID-000000000011",
                      "question": "Old old question 1",
                      "responses": [
                        {
                          "response": "Old old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 2"
                        }
                      ],
                      "additionalInformation": null
                    },
                    {
                      "code": "QID-000000000022",
                      "question": "Old old question 2",
                      "responses": [
                        {
                          "response": "Old old answer 1",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 1"
                        },
                        {
                          "response": "Old old answer 2",
                          "recordedBy": "user2",
                          "recordedOn": "2023-12-04T12:34:56",
                          "additionalInformation": "comment 2"
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
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "USER1"
                },
                {
                  "status": "IN_ANALYSIS",
                  "setOn": "2023-12-05T12:34:56",
                  "setBy": "OF42"
                }
              ],
              "staffInvolved": [
                {
                  "staffUsername": "user2",
                  "staffRole": "PRESENT_AT_SCENE",
                  "comment": "REPORTER"
                },
                {
                  "staffUsername": "JAMESQ",
                  "staffRole": "PRESENT_AT_SCENE",
                  "comment": "James was also present actually"
                }
              ],
              "prisonersInvolved": [
                {
                  "prisonerNumber": "B2222BB",
                  "prisonerRole": "HOSTAGE",
                  "outcome": "TRANSFER",
                  "comment": "Prisoner was transferred after incident"
                },
                {
                  "prisonerNumber": "A1234AA",
                  "prisonerRole": "PERPETRATOR",
                  "outcome": "LOCAL_INVESTIGATION",
                  "comment": "Trevor took another prisoner hostage"
                }
              ],
              "locations": [],
              "evidence": [],
              "correctionRequests": [
                {
                  "reason": "NOT_SPECIFIED",
                  "descriptionOfChange": "Also the description",
                  "correctionRequestedBy": "user2",
                  "correctionRequestedAt": "2023-12-05T00:00:00"
                },
                {
                  "reason": "NOT_SPECIFIED",
                  "descriptionOfChange": "Could you update the title please",
                  "correctionRequestedBy": "user2",
                  "correctionRequestedAt": "2023-11-28T00:00:00"
                }
              ],
              "reportedBy": "OF42",
              "reportedDate": "2023-12-04T12:34:56",
              "status": "IN_ANALYSIS",
              "assignedTo": "USER1",
              "createdDate": "2023-12-05T12:34:56",
              "lastModifiedDate": "2023-12-05T12:34:56",
              "lastModifiedBy": "OF42",
              "createdInNomis": true
            }
            """,
            true,
          )

        getDomainEvents(1).let {
          assertThat(it.map { message -> message.eventType to Pair(message.additionalInformation?.id, message.additionalInformation?.source) }).containsExactlyInAnyOrder(
            "incident.report.amended" to Pair(existingNomisReport.id, InformationSource.NOMIS),
          )
        }
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

    private fun assertNoDomainMessagesSent() {
      assertThat(getNumberOfMessagesCurrentlyOnSubscriptionQueue()).isZero
    }

    private fun assertCreatedReportDomainMessageSent() {
      assertThat(getDomainEvents(1)).allMatch { event ->
        event.eventType == "incident.report.created" &&
          event.additionalInformation?.let { additionalInformation ->
            additionalInformation.id != existingNomisReport.id && // note that ids should not match
              additionalInformation.source == InformationSource.NOMIS
          } ?: false
      }
    }

    private fun assertAmendedReportDomainMessageSent() {
      assertThat(getDomainEvents(1)).allMatch { event ->
        event.eventType == "incident.report.amended" &&
          event.additionalInformation?.let { additionalInformation ->
            additionalInformation.id == existingNomisReport.id &&
              additionalInformation.source == InformationSource.NOMIS
          } ?: false
      }
    }

    @Test
    fun `can create a report during initial migration`() {
      deleteAllReports() // drop reports from test setup to prevent incident number and event id clashes

      sendAuthorisedSyncRequest(
        initialMigration = true,
        incidentIdToUpdate = null,
      ) {
        // new report created
        expectStatus().isCreated

        // no domain events sent because migrating from NOMIS
        assertNoDomainMessagesSent()
      }
    }

    @Test
    fun `can create a report after initial migration`() {
      deleteAllReports() // drop reports from test setup to prevent incident number and event id clashes

      sendAuthorisedSyncRequest(
        initialMigration = false,
        incidentIdToUpdate = null,
      ) {
        // new report created
        expectStatus().isCreated

        // already migrated, so domain event should be raised
        assertCreatedReportDomainMessageSent()
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

        // no domain events sent because migrating from NOMIS
        assertNoDomainMessagesSent()
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

        // already migrated, so domain event should be raised
        assertAmendedReportDomainMessageSent()
      }
    }
  }
}
