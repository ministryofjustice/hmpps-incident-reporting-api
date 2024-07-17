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
import org.springframework.test.util.JsonExpectationsHelper
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
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
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.time.Clock
import java.time.LocalDate
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
            reportingStaff,
            NomisCode("PAS", "Present at scene"),
            "REPORTER",
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
            role = NomisCode("PERP", "Perpetrator"),
            outcome = NomisCode("ACCT", "ACCT"),
            comment = "Comment",
            createDateTime = now,
            createdBy = reportingStaff.username,
          ),
        ),
        requirements = listOf(
          NomisRequirement(
            "Change 1",
            LocalDate.now(clock),
            reportingStaff,
            "MDI",
            createDateTime = now,
            createdBy = reportingStaff.username,
          ),
          NomisRequirement(
            "Change 2",
            LocalDate.now(clock).minusWeeks(1),
            reportingStaff,
            "MDI",
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
            "Question 1",
            listOf(
              NomisResponse(
                10,
                1,
                "Answer 1",
                "comment 1",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                11,
                2,
                "Answer 2",
                "comment 2",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                12,
                3,
                "Answer 3",
                "comment 3",
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
            "Question 2",
            listOf(
              NomisResponse(
                13,
                1,
                "Answer 1",
                "comment 1",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                14,
                2,
                "Answer 2",
                "comment 2",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                15,
                3,
                "Answer 3",
                "comment 3",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
            ),
          ),
          NomisQuestion(
            6,
            3,
            createDateTime = now,
            createdBy = reportingStaff.username,
            "Question 3",
            listOf(
              NomisResponse(
                16,
                1,
                "Answer 1",
                "comment 1",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                17,
                2,
                "Answer 2",
                "comment 2",
                createDateTime = now,
                createdBy = reportingStaff.username,
                recordingStaff = reportingStaff,
              ),
              NomisResponse(
                18,
                3,
                "Answer 3",
                "comment 3",
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
            createDateTime = now,
            createdBy = reportingStaff.username,
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
            JsonExpectationsHelper().assertJsonEqual(
              // language=json
              """
              {
                "reportReference": "112414666",
                "type": "SELF_HARM",
                "incidentDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "A New Incident From NOMIS",
                "event": {
                  "eventReference": "112414666",
                  "eventDateAndTime": "2023-12-05T11:34:56",
                  "prisonId": "MDI",
                  "title": "An incident occurred updated",
                  "description": "A New Incident From NOMIS",
                  "createdAt": "2023-12-05T14:34:56",
                  "modifiedAt": "2023-12-05T17:34:56",
                  "modifiedBy": "another-user"
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
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Answer 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Answer 3",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
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
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Answer 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Answer 3",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
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
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Answer 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Answer 3",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 3"
                      }
                    ]
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE",
                    "changedAt": "2023-12-05T00:00:00",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "QID-000000000001",
                        "question": "Old question 1",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 2"
                          },
                          {
                            "response": "Old answer 3",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
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
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 2"
                          },
                          {
                            "response": "Old answer 3",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
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
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 2"
                          },
                          {
                            "response": "Old answer 3",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
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
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2"
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
                "reportedAt": "2023-12-05T12:34:56",
                "status": "AWAITING_ANALYSIS",
                "assignedTo": "user2",
                "createdAt": "2023-12-05T14:34:56",
                "modifiedAt": "2023-12-05T17:34:56",
                "modifiedBy": "another-user",
                "createdInNomis": true
              }
              """,
              reportJson,
              false,
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
            JsonExpectationsHelper().assertJsonEqual(
              // language=json
              """
              {
                "reportReference": "$newIncidentId",
                "type": "ASSAULT",
                "incidentDateAndTime": "2023-12-05T11:34:56",
                "prisonId": "MDI",
                "title": "An incident occurred updated",
                "description": "New NOMIS incident",
                "event": {
                  "eventReference": "$newIncidentId",
                  "eventDateAndTime": "2023-12-05T11:34:56",
                  "prisonId": "MDI",
                  "title": "An incident occurred updated",
                  "description": "New NOMIS incident",
                  "createdAt": "2023-12-05T14:34:56",
                  "modifiedAt": "2023-12-05T17:34:56",
                  "modifiedBy": "another-user"
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
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Answer 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Answer 3",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
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
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Answer 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Answer 3",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
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
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Answer 2",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Answer 3",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-05T12:34:56",
                        "additionalInformation": "comment 3"
                      }
                    ]
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE",
                    "changedAt": "2023-12-05T00:00:00",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "QID-000000000001",
                        "question": "Old question 1",
                        "additionalInformation": null,
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 2"
                          },
                          {
                            "response": "Old answer 3",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
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
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 2"
                          },
                          {
                            "response": "Old answer 3",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
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
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
                            "additionalInformation": "comment 2"
                          },
                          {
                            "response": "Old answer 3",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-05T12:34:56",
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
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "user2"
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
                "reportedAt": "2023-12-05T12:34:56",
                "status": "AWAITING_ANALYSIS",
                "assignedTo": "user2",
                "createdAt": "2023-12-05T14:34:56",
                "modifiedAt": "2023-12-05T17:34:56",
                "modifiedBy": "another-user",
                "createdInNomis": true
              }
              """,
              reportJson,
              false,
            )
          }

        assertThatDomainEventWasSent("incident.report.created", "$newIncidentId", InformationSource.NOMIS)
      }

      @Test
      fun `can sync an update to an existing incident report created in NOMIS`() {
        val upsertMigration = syncRequest.copy(
          initialMigration = false,
          id = existingNomisReport.id,
          incidentReport = syncRequest.incidentReport.copy(
            incidentId = NOMIS_INCIDENT_NUMBER,
            title = "Updated title",
            description = "Updated details",
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
                reportingStaff,
                NomisCode("PAS", "Present at scene"),
                "REPORTER",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
              NomisStaffParty(
                NomisStaff("JAMESQ", 2, "James", "Quids"),
                NomisCode("PAS", "Present at scene"),
                "James was also present actually",
                createDateTime = now,
                createdBy = reportingStaff.username,
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
                createDateTime = now,
                createdBy = reportingStaff.username,
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
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
            ),
            requirements = listOf(
              NomisRequirement(
                "Also the description",
                LocalDate.now(clock),
                reportingStaff,
                "MDI",
                createDateTime = now,
                createdBy = reportingStaff.username,
              ),
              NomisRequirement(
                "Could you update the title please",
                LocalDate.now(clock).minusWeeks(1),
                reportingStaff,
                "MDI",
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
                    1,
                    "John",
                    "comment 1",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    11,
                    2,
                    "Trevor",
                    "comment 2",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    12,
                    3,
                    "Maybe someone else?",
                    "comment 3",
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
                    1,
                    "Cell",
                    "comment 1",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    14,
                    2,
                    "Landing",
                    "comment 2",
                    createDateTime = now,
                    createdBy = reportingStaff.username,
                    recordingStaff = reportingStaff,
                  ),
                  NomisResponse(
                    15,
                    3,
                    "Kitchen",
                    "comment 3",
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
                incidentChangeDate = LocalDate.now(clock).minusDays(2),
                incidentChangeStaff = reportingStaff,
                createDateTime = now,
                createdBy = reportingStaff.username,
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
                incidentChangeDate = LocalDate.now(clock).minusDays(1),
                incidentChangeStaff = reportingStaff,
                createDateTime = now,
                createdBy = reportingStaff.username,
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
          .bodyValue(upsertMigration.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath("id").value<String> {
            val reportId = UUID.fromString(it)
            val report = reportRepository.findOneEagerlyById(reportId)!!.toDtoWithDetails()
            val reportJson = report.toJson()
            JsonExpectationsHelper().assertJsonEqual(
              // language=json
              """
              {
                "id": "${existingNomisReport.id}",
                "reportReference": "$NOMIS_INCIDENT_NUMBER",
                "type": "ASSAULT",
                "incidentDateAndTime": "2023-11-25T12:34:56",
                "prisonId": "FBI",
                "title": "Updated title",
                "description": "Updated details",
                "event": {
                  "eventReference": "$NOMIS_INCIDENT_NUMBER",
                  "eventDateAndTime": "2023-11-25T12:34:56",
                  "prisonId": "FBI",
                  "title": "Updated title",
                  "description": "Updated details",
                  "createdAt": "2023-12-04T12:34:56",
                  "modifiedAt": "2023-12-05T12:29:56",
                  "modifiedBy": "updater"
                },
                "questions": [
                  {
                    "code": "QID-000000000004",
                    "question": "Who was involved?",
                    "responses": [
                      {
                        "response": "John",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Trevor",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Maybe someone else?",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56",
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
                        "recordedAt": "2023-12-04T12:34:56",
                        "additionalInformation": "comment 1"
                      },
                      {
                        "response": "Landing",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56",
                        "additionalInformation": "comment 2"
                      },
                      {
                        "response": "Kitchen",
                        "recordedBy": "user2",
                        "recordedAt": "2023-12-04T12:34:56",
                        "additionalInformation": "comment 3"
                      }
                    ],
                    "additionalInformation": null
                  }
                ],
                "history": [
                  {
                    "type": "DAMAGE",
                    "changedAt": "2023-12-03T00:00:00",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "QID-000000000001",
                        "question": "Old question 1",
                        "responses": [
                          {
                            "response": "Old answer 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56",
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
                            "recordedAt": "2023-12-04T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56",
                            "additionalInformation": "comment 2"
                          }
                        ],
                        "additionalInformation": null
                      }
                    ]
                  },
                  {
                    "type": "BOMB_THREAT",
                    "changedAt": "2023-12-04T00:00:00",
                    "changedBy": "user2",
                    "questions": [
                      {
                        "code": "QID-000000000011",
                        "question": "Old old question 1",
                        "responses": [
                          {
                            "response": "Old old answer 1",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56",
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
                            "recordedAt": "2023-12-04T12:34:56",
                            "additionalInformation": "comment 1"
                          },
                          {
                            "response": "Old old answer 2",
                            "recordedBy": "user2",
                            "recordedAt": "2023-12-04T12:34:56",
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
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "USER1"
                  },
                  {
                    "status": "IN_ANALYSIS",
                    "changedAt": "2023-12-05T12:34:56",
                    "changedBy": "updater"
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
                "reportedAt": "2023-12-04T12:34:56",
                "status": "IN_ANALYSIS",
                "assignedTo": "USER1",
                "createdAt": "2023-12-04T12:34:56",
                "modifiedAt": "2023-12-05T12:29:56",
                "modifiedBy": "updater",
                "createdInNomis": true
              }
              """,
              reportJson,
              true,
            )
          }

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "$NOMIS_INCIDENT_NUMBER",
          InformationSource.NOMIS,
          WhatChanged.ANYTHING,
        )
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
      deleteAllReports() // drop reports from test setup to prevent report and event reference clashes

      sendAuthorisedSyncRequest(
        initialMigration = true,
        incidentIdToUpdate = null,
      ) {
        // new report created
        expectStatus().isCreated

        // no domain events sent because migrating from NOMIS
        assertThatNoDomainEventsWereSent()
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
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains(
              "Cannot update an existing report (${existingNomisReport.id}) during initial migration",
            )
          }

        // no domain events sent because migrating from NOMIS
        assertThatNoDomainEventsWereSent()
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
