package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.util.JsonExpectationsHelper
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.time.Clock
import java.util.UUID

@DisplayName("Report resource")
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

  protected fun assertThatReportWasModified(id: UUID) {
    webTestClient.get().uri("/incident-reports/$id")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectBody().json(
        // language=json
        """
          {
            "lastModifiedInNomis": false,
            "modifiedAt": "2023-12-05T12:34:56",
            "modifiedBy": "request-user"
          }
          """,
        false,
      )
  }

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    existingReport = reportRepository.save(
      buildReport(
        reportReference = "11124143",
        reportTime = now,
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
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
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

      @Test
      fun `cannot sort by invalid property`() {
        webTestClient.get().uri("$url?sort=missing,DESC")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("No property 'missing' found for type 'Report'")
          }
      }

      @ParameterizedTest(name = "cannot filter by invalid `{0}`")
      @ValueSource(
        strings = [
          "location=M",
          "location=Moorland+(HMP and YOI)",
          "source=nomis",
          "status=new",
          "type=ABSCOND",
          "incidentDateFrom=2024",
          "incidentDateUntil=yesterday",
          "reportedDateFrom=1%2F1%2F2020",
          "reportedByUsername=ab",
          "involvingStaffUsername=ab",
          "involvingPrisonerNumber=A1111",
          // TODO: `prisonId` tests can be removed once NOMIS reconciliation checks are updated to use `location`
          "prisonId=",
        ],
      )
      fun `cannot filter by invalid property`(param: String) {
        webTestClient.get().uri("$url?$param")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isBadRequest
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
              "totalPages": 0,
              "sort": ["incidentDateAndTime,DESC"]
            }""",
            true,
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
                "reportReference": "11124143",
                "incidentDateAndTime": "2023-12-05T11:34:56"
              }],
              "number": 0,
              "size": 20,
              "numberOfElements": 1,
              "totalElements": 1,
              "totalPages": 1,
              "sort": ["incidentDateAndTime,DESC"]
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
          reportRepository.saveAll(
            listOf("11017203", "11006603", "94728", "31934")
              .mapIndexed { index, reportReference ->
                val fromDps = reportReference.length >= 8
                val daysBefore = index.toLong() + 1
                buildReport(
                  reportReference = reportReference,
                  reportTime = now.minusDays(daysBefore),
                  reportingUsername = if (index < 2) "USER1" else "USER2",
                  location = if (index < 2) "LEI" else "MDI",
                  status = if (fromDps) Status.DRAFT else Status.AWAITING_ANALYSIS,
                  source = if (fromDps) InformationSource.DPS else InformationSource.NOMIS,
                  type = if (index < 3) Type.FINDS else Type.FIRE,
                  generateStaffInvolvement = index,
                  generatePrisonerInvolvement = index,
                )
              },
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
                "content": [
                  {
                    "reportReference": "11124143",
                    "incidentDateAndTime": "2023-12-05T11:34:56"
                  },
                  {
                    "reportReference": "11017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56"
                  },
                  {
                    "reportReference": "11006603",
                    "incidentDateAndTime": "2023-12-03T11:34:56"
                  },
                  {
                    "reportReference": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56"
                  },
                  {
                    "reportReference": "31934",
                    "incidentDateAndTime": "2023-12-01T11:34:56"
                  }
                ],
                "number": 0,
                "size": 20,
                "numberOfElements": 5,
                "totalElements": 5,
                "totalPages": 1,
                "sort": ["incidentDateAndTime,DESC"]
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
                    "reportReference": "11124143",
                    "incidentDateAndTime": "2023-12-05T11:34:56"
                  },
                  {
                    "reportReference": "11017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56"
                  }
                ],
                "number": 0,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["incidentDateAndTime,DESC"]
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
                    "reportReference": "11006603",
                    "incidentDateAndTime": "2023-12-03T11:34:56"
                  },
                  {
                    "reportReference": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56"
                  }
                ],
                "number": 1,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              false,
            )
        }

        @ParameterizedTest(name = "can sort reports by {0}")
        @ValueSource(
          strings = [
            "incidentDateAndTime,ASC",
            "incidentDateAndTime,DESC",
            "reportReference,ASC",
            "reportReference,DESC",
            "id,ASC",
            "id,DESC",
          ],
        )
        fun `can sort reports`(sortParam: String) {
          val expectedReportReferences = mapOf(
            "incidentDateAndTime,ASC" to listOf("31934", "94728", "11006603", "11017203", "11124143"),
            "incidentDateAndTime,DESC" to listOf("11124143", "11017203", "11006603", "94728", "31934"),
            "reportReference,ASC" to listOf("11006603", "11017203", "11124143", "31934", "94728"),
            "reportReference,DESC" to listOf("94728", "31934", "11124143", "11017203", "11006603"),
            // id, being a UUIDv7, should follow table insertion order (i.e. what setUp methods do above)
            "id,ASC" to listOf("11124143", "11017203", "11006603", "94728", "31934"),
            "id,DESC" to listOf("31934", "94728", "11006603", "11017203", "11124143"),
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
                "totalPages": 1,
                "sort": ["$sortParam"]
              }""",
              false,
            ).jsonPath("content[*].reportReference").value<List<String>> {
              assertThat(it).isEqualTo(expectedReportReferences)
            }
        }

        @ParameterizedTest(name = "can filter reports by `{0}`")
        @CsvSource(
          value = [
            "''                                                          | 5",
            "location=                                                   | 5",
            "location=MDI                                                | 3",
            "location=LEI,MDI                                            | 5",
            "location=MDI&location=LEI                                   | 5",
            "location=BXI                                                | 0",
            "status=                                                     | 5",
            "status=DRAFT                                                | 3",
            "status=AWAITING_ANALYSIS                                    | 2",
            "status=AWAITING_ANALYSIS,DRAFT                              | 5",
            "status=AWAITING_ANALYSIS&status=DRAFT                       | 5",
            "status=AWAITING_ANALYSIS,DRAFT&source=DPS                   | 3",
            "status=CLOSED                                               | 0",
            "source=DPS                                                  | 3",
            "source=NOMIS                                                | 2",
            "source=NOMIS&location=MDI                                   | 2",
            "source=DPS&location=MDI                                     | 1",
            "type=ASSAULT                                                | 0",
            "type=FIRE                                                   | 1",
            "type=FIRE&location=LEI                                      | 0",
            "type=FIRE&location=MDI                                      | 1",
            "incidentDateFrom=2023-12-05                                 | 1",
            "incidentDateFrom=2023-12-04                                 | 2",
            "incidentDateFrom=2023-12-03                                 | 3",
            "incidentDateUntil=2023-12-03                                | 3",
            "incidentDateUntil=2023-12-02                                | 2",
            "incidentDateFrom=2023-12-02&incidentDateUntil=2023-12-02    | 1",
            "reportedByUsername=USER2                                    | 2",
            "involvingStaffUsername=staff-1                              | 3",
            "involvingStaffUsername=staff-3                              | 1",
            "involvingStaffUsername=staff-1&incidentDateFrom=2023-12-03  | 1",
            "involvingPrisonerNumber=A0001AA                             | 3",
            "involvingPrisonerNumber=A0002AA                             | 2",
            "involvingPrisonerNumber=A0001AA&incidentDateFrom=2023-12-03 | 1",
            // TODO: `prisonId` tests can be removed once NOMIS reconciliation checks are updated to use `location`
            "prisonId=MDI                                                | 3",
            "location=LEI,MDI&prisonId=MDI                               | 5",
          ],
          delimiter = '|',
        )
        fun `can filter reports`(params: String, count: Int) {
          webTestClient.get().uri("$url?$params")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("totalElements").isEqualTo(count)
        }
      }
    }
  }

  @DisplayName("GET /incident-reports/{id}")
  @Nested
  inner class GetBasicReportById {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by ID if it is not found`() {
        webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
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
              "reportReference": "11124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-reports/{id}/with-details")
  @Nested
  inner class GetReportWithDetailsById {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}/with-details"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by ID if it is not found`() {
        webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555/with-details")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
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
              "reportReference": "11124143",
              "type": "FINDS",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "event": {
                "id": "${existingReport.event.id}",
                "eventReference": "11124143",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An event occurred",
                "description": "Details of the event",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "nomisStatus": null,
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-reports/reference/{reference}")
  @Nested
  inner class GetBasicReportByReference {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/reference/${existingReport.reportReference}"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by reference if it is not found`() {
        webTestClient.get().uri("/incident-reports/reference/11111111")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can get a report by reference`() {
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
              "reportReference": "11124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-reports/reference/{reference}/with-details")
  @Nested
  inner class GetReportWithDetailsByReference {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/reference/${existingReport.reportReference}/with-details"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        "VIEW_INCIDENT_REPORTS",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot get a report by reference if it is not found`() {
        webTestClient.get().uri("/incident-reports/reference/11111111/with-details")
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can get a report by reference`() {
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
              "reportReference": "11124143",
              "type": "FINDS",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "event": {
                "id": "${existingReport.event.id}",
                "eventReference": "11124143",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An event occurred",
                "description": "Details of the event",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "nomisStatus": null,
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false
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
      incidentDateAndTime = now.minusHours(1),
      title = "An incident occurred",
      description = "Longer explanation of incident",
      type = Type.SELF_HARM,
      location = "MDI",
      createNewEvent = true,
    )

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.post().uri(url).bodyValue(createReportRequest.toJson()),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot create a report with invalid payload`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            "{}",
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @ParameterizedTest(name = "cannot create a report with invalid `{0}` field")
      @ValueSource(strings = ["location", "title", "description"])
      fun `cannot create a report with invalid fields`(fieldName: String) {
        val invalidPayload = createReportRequest.copy(
          location = if (fieldName == "location") "" else createReportRequest.location,
          title = if (fieldName == "title") "" else createReportRequest.title,
          description = if (fieldName == "description") "" else createReportRequest.description,
        ).toJson()
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains(fieldName)
          }

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot create a report for future time`() {
        val invalidPayload = createReportRequest.copy(
          incidentDateAndTime = now.plusMinutes(10),
        ).toJson()
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("incidentDateAndTime cannot be in the future")
          }

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot create a report without creating or linking event`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.copy(createNewEvent = false).toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Either createNewEvent or linkedEventReference must be provided")
          }

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot create a report with an inactive type`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.copy(type = Type.OLD_ASSAULT).toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Inactive incident type OLD_ASSAULT")
          }

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can add a new incident report`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.toJson())
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
            {
              "type": "SELF_HARM",
              "nomisType": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "An incident occurred",
              "description": "Longer explanation of incident",
              "event": {
                "eventDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An incident occurred",
                "description": "Longer explanation of incident",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "request-user"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "request-user"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "reportedBy": "request-user",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "nomisStatus": null,
              "assignedTo": "request-user",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            false,
          )

        assertThatDomainEventWasSent("incident.report.created", null)
      }

      @Test
      fun `can add a new incident linked to an existing event`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            createReportRequest.copy(
              createNewEvent = false,
              linkedEventReference = existingReport.event.eventReference,
            ).toJson(),
          )
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """ 
            {
              "type": "SELF_HARM",
              "nomisType": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "An incident occurred",
              "description": "Longer explanation of incident",
              "event": {
                "eventReference": "${existingReport.event.eventReference}",
                "eventDateAndTime": "2023-12-05T11:34:56",
                "location": "MDI",
                "prisonId": "MDI",
                "title": "An event occurred",
                "description": "Details of the event",
                "createdAt": "2023-12-05T12:34:56",
                "modifiedAt": "2023-12-05T12:34:56",
                "modifiedBy": "USER1"
              },
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "request-user"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "reportedBy": "request-user",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "nomisStatus": null,
              "assignedTo": "request-user",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            false,
          )

        assertThatDomainEventWasSent("incident.report.created", null)
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}")
  @Nested
  inner class UpdateReport {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.patch().uri(url).bodyValue(UpdateReportRequest().toJson()),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot update a report with invalid payload`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            "[]",
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @ParameterizedTest(name = "cannot update a report with invalid `{0}` field")
      @ValueSource(strings = ["location", "title", "description"])
      fun `cannot update a report with invalid fields`(fieldName: String) {
        val invalidPayload = UpdateReportRequest(
          location = if (fieldName == "location") "" else null,
          title = if (fieldName == "title") "" else null,
          description = if (fieldName == "description") "" else null,
        ).toJson()
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload.toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains(fieldName)
          }

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot update a report to a future time`() {
        val invalidPayload = UpdateReportRequest(
          incidentDateAndTime = now.plusMinutes(10),
        ).toJson()
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(invalidPayload)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("incidentDateAndTime cannot be in the future")
          }

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot update a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(UpdateReportRequest().toJson())
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can update an incident report with no changes`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(UpdateReportRequest().toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            true,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can update all incident report fields`() {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = now.minusHours(2),
          location = "LEI",
          title = "Updated report 11124143",
          description = "Updated incident report of type Finds",
        )
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FINDS",
              "incidentDateAndTime": "2023-12-05T10:34:56",
              "location": "LEI",
              "prisonId": "LEI",
              "title": "Updated report 11124143",
              "description": "Updated incident report of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            true,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
      }

      @ParameterizedTest(name = "can update `{0}` of an incident report")
      @ValueSource(strings = ["incidentDateAndTime", "location", "title", "description"])
      fun `can update an incident report field`(fieldName: String) {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = if (fieldName == "incidentDateAndTime") now.minusHours(2) else null,
          location = if (fieldName == "location") "LEI" else null,
          title = if (fieldName == "title") "Updated report 11124143" else null,
          description = if (fieldName == "description") "Updated incident report of type Finds" else null,
        )
        val expectedIncidentDateAndTime = if (fieldName == "incidentDateAndTime") {
          "2023-12-05T10:34:56"
        } else {
          "2023-12-05T11:34:56"
        }
        val expectedLocation = if (fieldName == "location") {
          "LEI"
        } else {
          "MDI"
        }
        val expectedTitle = if (fieldName == "title") {
          "Updated report 11124143"
        } else {
          "Incident Report 11124143"
        }
        val expectedDescription = if (fieldName == "description") {
          "Updated incident report of type Finds"
        } else {
          "A new incident created in the new service of type Finds"
        }
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FINDS",
              "incidentDateAndTime": "$expectedIncidentDateAndTime",
              "location": "$expectedLocation",
              "prisonId": "$expectedLocation",
              "title": "$expectedTitle",
              "description": "$expectedDescription",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false
            }
            """,
            true,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
      }

      @ParameterizedTest(name = "can propagate updates to parent event when requested: {0}")
      @ValueSource(booleans = [true, false])
      fun `can propagate updates to parent event when requested`(updateEvent: Boolean) {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = now.minusHours(2),
          location = "LEI",
          title = "Updated report 11124143",
          description = "Updated incident report of type Finds",

          updateEvent = updateEvent,
        )
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk

        val eventJson = eventRepository.findOneByEventReference("11124143")!!
          .toDto().toJson()
        JsonExpectationsHelper().assertJsonEqual(
          if (updateEvent) {
            // language=json
            """
            {
              "eventReference": "11124143",
              "eventDateAndTime": "2023-12-05T10:34:56",
              "location": "LEI",
              "prisonId": "LEI",
              "title": "Updated report 11124143",
              "description": "Updated incident report of type Finds",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user"
            }
            """
          } else {
            // language=json
            """
            {
              "eventReference": "11124143",
              "eventDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "An event occurred",
              "description": "Details of the event",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1"
            }
            """
          },
          eventJson,
          false,
        )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          InformationSource.DPS,
          WhatChanged.BASIC_REPORT,
        )
      }

      @Test
      fun `can update a report first created in NOMIS`() {
        val nomisReport = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now,
            source = InformationSource.NOMIS,
          ),
        )
        val updateReportRequest = UpdateReportRequest(
          description = "Updated description",
        )
        webTestClient.patch().uri("/incident-reports/${nomisReport.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${nomisReport.id}",
              "description": "Updated description",
              "createdInNomis": true,
              "lastModifiedInNomis": false
            }
            """,
            false,
          )
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}/status")
  @Nested
  inner class ChangeStatus {
    private lateinit var url: String

    // language=json
    private val validPayload = """
      {"newStatus": "AWAITING_ANALYSIS"}
    """

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}/status"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.patch().uri(url).bodyValue(validPayload),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot change status of a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555/status")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change status of a report with invalid payload`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"status": "CLOSED"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change status of a report to an unknown one`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newStatus": "OPEN"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can change status of a report but keeping it the same`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newStatus": "DRAFT"}
            """,
          )
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FINDS",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "DRAFT",
              "nomisStatus": null,
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                }
              ]
            }
            """,
            false,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can change status of a report preserving history`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FINDS",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56",
              "status": "AWAITING_ANALYSIS",
              "nomisStatus": "AWAN",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "USER1"
                },
                {
                  "status": "AWAITING_ANALYSIS",
                  "nomisStatus": "AWAN",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "request-user"
                }
              ]
            }
            """,
            false,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          InformationSource.DPS,
          WhatChanged.STATUS,
        )
      }

      @Test
      fun `can change status of a report first created in NOMIS`() {
        val nomisReport = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now,
            source = InformationSource.NOMIS,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${nomisReport.id}/status")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${nomisReport.id}",
              "status": "AWAITING_ANALYSIS",
              "createdInNomis": true,
              "lastModifiedInNomis": false
            }
            """,
            false,
          )
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}/type")
  @Nested
  inner class ChangeType {
    private lateinit var urlWithNoQuestionsOrHistory: String

    // language=json
    private val validPayload = """
      {"newType": "FIRE"}
    """

    @BeforeEach
    fun setUp() {
      urlWithNoQuestionsOrHistory = "/incident-reports/${existingReport.id}/type"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory).bodyValue(validPayload),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot change type of a missing report`() {
        webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change type of a report with invalid payload`() {
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"type": "FIRE"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change type of a report to an unknown one`() {
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newType": "MISSING_TYPE"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `cannot change type of a report to inactive one`() {
        webTestClient.patch().uri(urlWithNoQuestionsOrHistory)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newType": "OLD_ASSAULT3"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Inactive incident type OLD_ASSAULT3")
          }

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can change type of a report but keeping it the same`() {
        val reportWithQuestions = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_ANALYSIS,
            generateQuestions = 2,
            generateResponses = 2,
            generateHistory = 0,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${reportWithQuestions.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(
            // language=json
            """
              {"newType": "FINDS"}
            """,
          )
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${reportWithQuestions.id}",
              "reportReference": "11124146",
              "type": "FINDS",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:31:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124146",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56",
              "status": "AWAITING_ANALYSIS",
              "nomisStatus": "AWAN",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:31:56",
              "modifiedAt": "2023-12-05T12:31:56",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "history": [],
              "questions": [
                {
                  "code": "1",
                  "question": "Question #1",
                  "additionalInformation": "Explanation #1",
                  "responses": [
                    {
                      "response": "Response #1",
                      "responseDate": "2023-12-04",
                      "additionalInformation": "Prose #1",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    },
                    {
                      "response": "Response #2",
                      "responseDate": "2023-12-03",
                      "additionalInformation": "Prose #2",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    }
                  ]
                },
                {
                  "code": "2",
                  "question": "Question #2",
                  "additionalInformation": "Explanation #2",
                  "responses": [
                    {
                      "response": "Response #1",
                      "responseDate": "2023-12-04",
                      "additionalInformation": "Prose #1",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    },
                    {
                      "response": "Response #2",
                      "responseDate": "2023-12-03",
                      "additionalInformation": "Prose #2",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56"
                    }
                  ]
                }
              ]
            }
            """,
            false,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can change type of a report creating history when there was none`() {
        val reportWithQuestions = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_ANALYSIS,
            generateQuestions = 2,
            generateResponses = 2,
            generateHistory = 0,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${reportWithQuestions.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${reportWithQuestions.id}",
              "reportReference": "11124146",
              "type": "FIRE",
              "nomisType": "FIRE",
              "incidentDateAndTime": "2023-12-05T11:31:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124146",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56",
              "status": "AWAITING_ANALYSIS",
              "nomisStatus": "AWAN",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:31:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "history": [
                {
                  "type": "FINDS",
                  "nomisType": "FIND0422",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "request-user",
                  "questions": [
                    {
                      "code": "1",
                      "question": "Question #1",
                      "additionalInformation": "Explanation #1",
                      "responses": [
                        {
                          "response": "Response #1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        },
                        {
                          "response": "Response #2",
                          "responseDate": "2023-12-03",
                          "additionalInformation": "Prose #2",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    },
                    {
                      "code": "2",
                      "question": "Question #2",
                      "additionalInformation": "Explanation #2",
                      "responses": [
                        {
                          "response": "Response #1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        },
                        {
                          "response": "Response #2",
                          "responseDate": "2023-12-03",
                          "additionalInformation": "Prose #2",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    }
                  ]
                }
              ],
              "questions": []
            }
            """,
            false,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124146",
          InformationSource.DPS,
          WhatChanged.TYPE,
        )
      }

      @Test
      fun `can change type of a report preserving history when it already existed`() {
        val reportWithQuestionsAndHistory = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_ANALYSIS,
            generateQuestions = 1,
            generateResponses = 1,
            generateHistory = 1,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${reportWithQuestionsAndHistory.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${reportWithQuestionsAndHistory.id}",
              "reportReference": "11124146",
              "type": "FIRE",
              "nomisType": "FIRE",
              "incidentDateAndTime": "2023-12-05T11:31:56",
              "location": "MDI",
              "prisonId": "MDI",
              "title": "Incident Report 11124146",
              "description": "A new incident created in the new service of type Finds",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56",
              "status": "AWAITING_ANALYSIS",
              "nomisStatus": "AWAN",
              "assignedTo": "USER1",
              "createdAt": "2023-12-05T12:31:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "history": [
                {
                  "type": "ASSAULT",
                  "nomisType": "ASSAULTS3",
                  "changedAt": "2023-12-05T12:31:56",
                  "changedBy": "some-past-user",
                  "questions": [
                    {
                      "code": "1-1",
                      "question": "Historical question #1-1",
                      "additionalInformation": "Explanation #1 in history #1",
                      "responses": [
                        {
                          "response": "Historical response #1-1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1 in history #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    }
                  ]
                },
                {
                  "type": "FINDS",
                  "nomisType": "FIND0422",
                  "changedAt": "2023-12-05T12:34:56",
                  "changedBy": "request-user",
                  "questions": [
                    {
                      "code": "1",
                      "question": "Question #1",
                      "additionalInformation": "Explanation #1",
                      "responses": [
                        {
                          "response": "Response #1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56"
                        }
                      ]
                    }
                  ]
                }
              ],
              "questions": []
            }
            """,
            false,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124146",
          InformationSource.DPS,
          WhatChanged.TYPE,
        )
      }

      @Test
      fun `can change type of a report first created in NOMIS`() {
        val nomisReport = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now,
            source = InformationSource.NOMIS,
          ),
        )
        webTestClient.patch().uri("/incident-reports/${nomisReport.id}/type")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "${nomisReport.id}",
              "type": "FIRE",
              "createdInNomis": true,
              "lastModifiedInNomis": false
            }
            """,
            false,
          )
      }
    }
  }

  @DisplayName("DELETE /incident-reports/{id}")
  @Nested
  inner class DeleteReport {
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"
    }

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.delete().uri(url),
        "MAINTAIN_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot delete a report by ID if it is not found`() {
        webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @ParameterizedTest(name = "can delete a report by ID (including orphaned event? {0})")
      @ValueSource(booleans = [true, false])
      fun `can delete a report by ID`(deleteOrphanedEvents: Boolean) {
        val reportId = existingReport.id!!
        val eventId = existingReport.event.id!!

        webTestClient.delete().uri("$url?deleteOrphanedEvents=$deleteOrphanedEvents")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "$reportId",
              "reportReference": "11124143"
            }
            """,
            false,
          )

        assertThat(reportRepository.findOneEagerlyById(reportId)).isNull()
        if (deleteOrphanedEvents) {
          assertThat(eventRepository.findById(eventId)).isEmpty
        } else {
          assertThat(eventRepository.findById(eventId)).isPresent
        }

        assertThatDomainEventWasSent("incident.report.deleted", "11124143")
      }

      @Test
      fun `cannot cascade deleting non-orphan event`() {
        val reportId = existingReport.id!!
        val eventId = existingReport.event.id!!

        existingReport.event.addReport(
          buildReport(
            reportReference = "11124142",
            reportTime = now.minusMinutes(5),
          ),
        )
        eventRepository.save(existingReport.event)
        val otherReportId = reportRepository.findAll().first { it.id != reportId }.id!!

        webTestClient.delete().uri("$url?deleteOrphanedEvents=true")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": "$reportId",
              "reportReference": "11124143"
            }
            """,
            false,
          )

        val remainingReportIds = reportRepository.findAllById(listOf(reportId, otherReportId)).map { it.id }
        assertThat(remainingReportIds).containsOnly(otherReportId)
        assertThat(eventRepository.findById(eventId)).isPresent
      }
    }
  }

  @Nested
  abstract inner class RelatedObjects(val urlSuffix: String, val whatChanged: WhatChanged? = null) {
    protected lateinit var existingReportWithRelatedObjects: Report
    protected lateinit var urlWithRelatedObjects: String
    protected lateinit var urlWithoutRelatedObjects: String
    protected lateinit var urlForFirstRelatedObject: String
    protected lateinit var urlForSecondRelatedObject: String

    @BeforeEach
    fun setUp() {
      urlWithoutRelatedObjects = "/incident-reports/${existingReport.id}/$urlSuffix"

      existingReportWithRelatedObjects = reportRepository.save(
        buildReport(
          reportReference = "11124146",
          reportTime = now,
          generateStaffInvolvement = 2,
          generatePrisonerInvolvement = 2,
          generateCorrections = 2,
        ),
      )
      urlWithRelatedObjects = "/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix"
      urlForFirstRelatedObject = "/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/1"
      urlForSecondRelatedObject = "/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/2"
    }

    abstract inner class ListObjects {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(urlWithRelatedObjects),
          "VIEW_INCIDENT_REPORTS",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot list objects for a report if it is not found`() {
          webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can list objects for a report when there are none`() {
          webTestClient.get().uri(urlWithoutRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              true,
            )
        }

        @Test
        fun `can list objects for a report when there are two`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/list-response.json")
          webTestClient.get().uri(urlWithRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )
        }
      }
    }

    abstract inner class AddObject(
      val invalidRequests: List<InvalidRequestTestCase> = emptyList(),
    ) {
      val validRequest = getResource("/related-objects/$urlSuffix/add-request.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.post().uri(urlWithRelatedObjects).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot add object to a report if it is not found`() {
          webTestClient.post().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @DisplayName("cannot add invalid object to a report")
        @TestFactory
        fun `cannot add invalid object to a report`(): List<DynamicTest> {
          val requests = mutableListOf(
            InvalidRequestTestCase(
              "empty request",
              // language=json
              "{}",
            ),
            InvalidRequestTestCase(
              "invalid shape",
              // language=json
              "[]",
              "Cannot deserialize value",
            ),
          )
          requests.addAll(invalidRequests)
          return requests.map { (name, request, expectedErrorText) ->
            DynamicTest.dynamicTest(name) {
              webTestClient.post().uri(urlWithoutRelatedObjects)
                .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody().jsonPath("developerMessage").value<String> {
                  if (expectedErrorText != null) {
                    assertThat(it).contains(expectedErrorText)
                  }
                }

              assertThatNoDomainEventsWereSent()
            }
          }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can add first object to a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/add-response-one.json")
          webTestClient.post().uri(urlWithoutRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReport.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124143",
            InformationSource.DPS,
            whatChanged,
          )
        }

        @Test
        fun `can add another object to a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/add-response-many.json")
          webTestClient.post().uri(urlWithRelatedObjects)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "11124146", InformationSource.DPS, whatChanged)
        }

        @Test
        fun `can add object to a report first created in NOMIS`() {
          val nomisReportWithRelatedObjects = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              source = InformationSource.NOMIS,
              generateStaffInvolvement = 2,
              generatePrisonerInvolvement = 2,
              generateCorrections = 2,
            ),
          )

          webTestClient.post().uri("/incident-reports/${nomisReportWithRelatedObjects.id}/$urlSuffix")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated

          val updatedNomisReportWithRelatedObjects = reportRepository.findOneByReportReference("11124147")!!.toDtoBasic()
          assertThat(updatedNomisReportWithRelatedObjects.createdInNomis).isTrue()
          assertThat(updatedNomisReportWithRelatedObjects.lastModifiedInNomis).isFalse()
        }
      }
    }

    abstract inner class UpdateObject(
      val invalidRequests: List<InvalidRequestTestCase> = emptyList(),
      val nullablePropertyRequests: List<NullablePropertyTestCase> = emptyList(),
    ) {
      val validRequest = getResource("/related-objects/$urlSuffix/update-request.json")
      val validPartialRequest = getResource("/related-objects/$urlSuffix/update-request-partial.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.patch().uri(urlForFirstRelatedObject).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot update related object for a report if it is not found`() {
          webTestClient.patch().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix/1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot update related object at index 0`() {
          webTestClient.patch().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/0")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 0 not found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot update related object beyond end`() {
          webTestClient.patch().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/3")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 3 not found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @DisplayName("cannot update related object with invalid payload")
        @TestFactory
        fun `cannot update related object with invalid payload`(): List<DynamicTest> {
          val requests = mutableListOf(
            InvalidRequestTestCase(
              "invalid shape",
              // language=json
              "[]",
              "Cannot deserialize value",
            ),
          )
          requests.addAll(invalidRequests)
          return requests.map { (name, request, expectedErrorText) ->
            DynamicTest.dynamicTest(name) {
              webTestClient.patch().uri(urlForFirstRelatedObject)
                .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody().jsonPath("developerMessage").value<String> {
                  if (expectedErrorText != null) {
                    assertThat(it).contains(expectedErrorText)
                  }
                }

              assertThatNoDomainEventsWereSent()
            }
          }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @ParameterizedTest(name = "can update {0} related object with no changes")
        @ValueSource(strings = ["first", "second"])
        fun `can update a related object with no changes`(index: String) {
          val expectedResponse = getResource("/related-objects/$urlSuffix/list-response.json")
          val url = if (index == "first") {
            urlForFirstRelatedObject
          } else {
            urlForSecondRelatedObject
          }
          webTestClient.patch().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(
              // language=json
              "{}",
            )
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatNoDomainEventsWereSent()
        }

        @ParameterizedTest(name = "can update {0} related object fully")
        @ValueSource(strings = ["first", "second"])
        fun `can update a related object fully`(index: String) {
          val expectedResponse = getResource("/related-objects/$urlSuffix/update-response-$index.json")
          val url = if (index == "first") {
            urlForFirstRelatedObject
          } else {
            urlForSecondRelatedObject
          }
          webTestClient.patch().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "11124146", InformationSource.DPS, whatChanged)
        }

        @ParameterizedTest(name = "can update {0} related object partially")
        @ValueSource(strings = ["first", "second"])
        fun `can update a related object partially`(index: String) {
          val expectedResponse = getResource("/related-objects/$urlSuffix/update-response-$index-partial.json")
          val url = if (index == "first") {
            urlForFirstRelatedObject
          } else {
            urlForSecondRelatedObject
          }
          webTestClient.patch().uri(url)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validPartialRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "11124146", InformationSource.DPS, whatChanged)
        }

        @DisplayName("can update nullable properties of a related object")
        @TestFactory
        fun `can update nullable properties of a related object`(): List<DynamicTest> {
          return nullablePropertyRequests.flatMap { testCase ->
            testCase.makeValidRequestTests().map { (name, request, expectedFieldValue) ->
              DynamicTest.dynamicTest(name) {
                webTestClient.patch().uri(urlForFirstRelatedObject)
                  .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                  .header("Content-Type", "application/json")
                  .bodyValue(request)
                  .exchange()
                  .expectStatus().isOk
                  .expectBody().jsonPath("[0].${testCase.field}").isEqualTo(expectedFieldValue)
              }
            }
          }
        }
      }

      @Test
      fun `can update related object for a report first created in NOMIS`() {
        val nomisReportWithRelatedObjects = reportRepository.save(
          buildReport(
            reportReference = "11124147",
            reportTime = now,
            source = InformationSource.NOMIS,
            generateStaffInvolvement = 2,
            generatePrisonerInvolvement = 2,
            generateCorrections = 2,
          ),
        )

        webTestClient.patch().uri("/incident-reports/${nomisReportWithRelatedObjects.id}/$urlSuffix/1")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isOk

        val updatedNomisReportWithRelatedObjects = reportRepository.findOneByReportReference("11124147")!!.toDtoBasic()
        assertThat(updatedNomisReportWithRelatedObjects.createdInNomis).isTrue()
        assertThat(updatedNomisReportWithRelatedObjects.lastModifiedInNomis).isFalse()
      }
    }

    abstract inner class RemoveObject {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.delete().uri(urlForFirstRelatedObject),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot delete related object from a report if it is not found`() {
          webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555/$urlSuffix/1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete related object at index 0`() {
          webTestClient.delete().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/0")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 0 not found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete related object beyond end`() {
          webTestClient.delete().uri("/incident-reports/${existingReportWithRelatedObjects.id}/$urlSuffix/3")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("index 3 not found")
            }

          assertThatNoDomainEventsWereSent()
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can delete first related object from a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/delete-response-first.json")
          webTestClient.delete().uri(urlForFirstRelatedObject)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "11124146", InformationSource.DPS, whatChanged)
        }

        @Test
        fun `can delete second related object from a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/delete-response-second.json")
          webTestClient.delete().uri(urlForSecondRelatedObject)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent("incident.report.amended", "11124146", InformationSource.DPS, whatChanged)
        }

        @Test
        fun `can delete related object from a report first created in NOMIS`() {
          val nomisReportWithRelatedObjects = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              source = InformationSource.NOMIS,
              generateStaffInvolvement = 2,
              generatePrisonerInvolvement = 2,
              generateCorrections = 2,
            ),
          )

          webTestClient.delete().uri("/incident-reports/${nomisReportWithRelatedObjects.id}/$urlSuffix/1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk

          val updatedNomisReportWithRelatedObjects = reportRepository.findOneByReportReference("11124147")!!.toDtoBasic()
          assertThat(updatedNomisReportWithRelatedObjects.createdInNomis).isTrue()
          assertThat(updatedNomisReportWithRelatedObjects.lastModifiedInNomis).isFalse()
        }
      }
    }
  }

  @DisplayName("Staff involvement")
  @Nested
  inner class StaffInvolvement : RelatedObjects("staff-involved", WhatChanged.STAFF_INVOLVED) {
    @DisplayName("GET /incident-reports/{reportId}/staff-involved")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/staff-involved")
    @Nested
    inner class AddObject : RelatedObjects.AddObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "short staff username",
          getResource("/related-objects/staff-involved/add-request-short-username.json"),
          "addStaffInvolvement.staffUsername: size must be between 3 and 120",
        ),
      ),
    )

    @DisplayName("PATCH /incident-reports/{reportId}/staff-involved/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "short staff username",
          getResource("/related-objects/staff-involved/update-request-short-username.json"),
          "updateStaffInvolvement.staffUsername: size must be between 3 and 120",
        ),
      ),
      nullablePropertyRequests = listOf(
        NullablePropertyTestCase(
          field = "comment",
          validValue = "Different comment",
          unchangedValue = "Comment #1",
        ),
      ),
    )

    @DisplayName("DELETE /incident-reports/{reportId}/staff-involved/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Prisoner involvement")
  @Nested
  inner class PrisonerInvolvement : RelatedObjects("prisoners-involved", WhatChanged.PRISONERS_INVOLVED) {
    @DisplayName("GET /incident-reports/{reportId}/prisoners-involved")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/prisoners-involved")
    @Nested
    inner class AddObject : RelatedObjects.AddObject()

    @DisplayName("PATCH /incident-reports/{reportId}/prisoners-involved/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject(
      nullablePropertyRequests = listOf(
        NullablePropertyTestCase(
          field = "outcome",
          validValue = "PLACED_ON_REPORT",
          unchangedValue = "CHARGED_BY_POLICE",
        ),
        NullablePropertyTestCase(
          field = "comment",
          validValue = "Different comment",
          unchangedValue = "Comment #1",
        ),
      ),
    )

    @DisplayName("DELETE /incident-reports/{reportId}/prisoners-involved/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Correction requests")
  @Nested
  inner class CorrectionRequests : RelatedObjects("correction-requests", WhatChanged.CORRECTION_REQUESTS) {
    @DisplayName("GET /incident-reports/{reportId}/correction-requests")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/correction-requests")
    @Nested
    inner class AddObject : RelatedObjects.AddObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "empty description of change",
          getResource("/related-objects/correction-requests/add-request-empty-description.json"),
          "addCorrectionRequest.descriptionOfChange: size must be between 1 and",
        ),
      ),
    )

    @DisplayName("PATCH /incident-reports/{reportId}/correction-requests/{index}")
    @Nested
    inner class UpdateObject : RelatedObjects.UpdateObject(
      invalidRequests = listOf(
        InvalidRequestTestCase(
          "empty description of change",
          getResource("/related-objects/correction-requests/update-request-empty-description.json"),
          "updateCorrectionRequest.descriptionOfChange: size must be between 1 and",
        ),
      ),
    )

    @DisplayName("DELETE /incident-reports/{reportId}/correction-requests/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Questions with responses")
  @Nested
  inner class QuestionsWithResponses {
    private lateinit var existingReportWithQuestionsAndResponses: Report
    private lateinit var urlWithoutQuestions: String
    private lateinit var urlWithQuestionsAndResponses: String

    @BeforeEach
    fun setUp() {
      urlWithoutQuestions = "/incident-reports/${existingReport.id}/questions"

      existingReportWithQuestionsAndResponses = reportRepository.save(
        buildReport(
          reportReference = "11124146",
          reportTime = now,
          generateQuestions = 2,
          generateResponses = 2,
        ),
      )
      urlWithQuestionsAndResponses = "/incident-reports/${existingReportWithQuestionsAndResponses.id}/questions"
    }

    @DisplayName("GET /incident-reports/{reportId}/questions")
    @Nested
    inner class ListQuestions {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(urlWithoutQuestions),
          "VIEW_INCIDENT_REPORTS",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot list questions and responses for a report if it is not found`() {
          webTestClient.get().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can list questions and responses when there are none`() {
          webTestClient.get().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              true,
            )
        }

        @Test
        fun `can list questions and responses when there are several`() {
          val expectedResponse = getResource("/questions-with-responses/list-response-with-several.json")
          webTestClient.get().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              expectedResponse,
              true,
            )
        }

        @Test
        fun `can list 50 questions and responses`() {
          val report = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              generateQuestions = 50,
              generateResponses = 3,
            ),
          )
          webTestClient.get().uri("/incident-reports/${report.id}/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$").value<List<Map<String, Any>>> { questions ->
              assertThat(questions).hasSize(50)
              assertThat(questions).allSatisfy { question ->
                val responses = question["responses"] as List<*>
                assertThat(responses).hasSize(3)
              }
            }
        }
      }
    }

    @DisplayName("POST /incident-reports/{reportId}/questions")
    @Nested
    inner class AddQuestions {
      private val validRequest = getResource("/questions-with-responses/add-request-with-responses.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.post().uri(urlWithQuestionsAndResponses).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot add question and responses to a report if it is not found`() {
          webTestClient.post().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @DisplayName("cannot add question and responses if payload is invalid")
        @TestFactory
        fun `cannot add question and responses if payload is invalid`(): List<DynamicTest> {
          return listOf(
            InvalidRequestTestCase(
              "invalid payload",
              // language=json
              "{}",
              "Cannot deserialize value",
            ),
            InvalidRequestTestCase(
              "empty list",
              // language=json
              "[]",
              "addQuestionWithResponses.addRequests: size must be between 1 and",
            ),
            InvalidRequestTestCase(
              "long code",
              getResource("/questions-with-responses/add-request-long-code.json"),
              "addQuestionWithResponses.addRequests[0].code: size must be between 1 and 60",
            ),
            InvalidRequestTestCase(
              "empty question",
              getResource("/questions-with-responses/add-request-empty-question.json"),
              "addQuestionWithResponses.addRequests[0].question: size must be between 1 and",
            ),
            InvalidRequestTestCase(
              "empty response",
              getResource("/questions-with-responses/add-request-empty-response.json"),
              "addQuestionWithResponses.addRequests[0].responses[1].response: size must be between 1 and",
            ),
          )
            .map { (name, request, expectedErrorText) ->
              DynamicTest.dynamicTest(name) {
                webTestClient.post().uri(urlWithoutQuestions)
                  .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
                  .header("Content-Type", "application/json")
                  .bodyValue(request)
                  .exchange()
                  .expectStatus().isBadRequest
                  .expectBody().jsonPath("developerMessage").value<String> {
                    if (expectedErrorText != null) {
                      assertThat(it).contains(expectedErrorText)
                    }
                  }

                assertThatNoDomainEventsWereSent()
              }
            }
        }

        @Test
        fun `cannot add question without responses`() {
          val invalidRequest = getResource("/questions-with-responses/add-request-without-responses.json")
          webTestClient.post().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("addQuestionWithResponses.addRequests[0].responses: size must be between 1 and")
            }

          assertThatNoDomainEventsWereSent()
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can add question with responses to empty report`() {
          val expectedResponse = getResource("/questions-with-responses/add-response-without-responses.json")
          webTestClient.post().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReport.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124143",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses to report with existing questions`() {
          val expectedResponse = getResource("/questions-with-responses/add-response-with-responses.json")
          webTestClient.post().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses that have nullable fields`() {
          val validRequestWithNulls = getResource("/questions-with-responses/add-request-with-responses-and-null-fields.json")
          val expectedResponse = getResource("/questions-with-responses/add-response-with-responses-and-null-fields.json")
          webTestClient.post().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequestWithNulls)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add multiple questions with responses in one go`() {
          val validRequestWith3Questions = getResource("/questions-with-responses/add-request-3-questions-with-responses.json")
          val expectedResponse = getResource("/questions-with-responses/add-response-3-questions-with-responses.json")
          webTestClient.post().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequestWith3Questions)
            .exchange()
            .expectStatus().isCreated
            .expectBody().json(
              expectedResponse,
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses to a report first created in NOMIS`() {
          val nomisReportWithQuestionsAndResponses = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              source = InformationSource.NOMIS,
              generateQuestions = 2,
              generateResponses = 2,
            ),
          )

          webTestClient.post().uri("/incident-reports/${nomisReportWithQuestionsAndResponses.id}/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isCreated

          val updatedNomisReportWithQuestionsAndResponses = reportRepository.findOneByReportReference("11124147")!!.toDtoBasic()
          assertThat(updatedNomisReportWithQuestionsAndResponses.createdInNomis).isTrue()
          assertThat(updatedNomisReportWithQuestionsAndResponses.lastModifiedInNomis).isFalse()
        }
      }
    }

    @DisplayName("DELETE /incident-reports/{reportId}/questions")
    @Nested
    inner class DeleteQuestion {
      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.delete().uri(urlWithQuestionsAndResponses),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot delete last question from a report if it is not found`() {
          webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("There is no report found")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete question from a report when the question list is empty`() {
          webTestClient.delete().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("Question list is empty")
            }

          assertThatNoDomainEventsWereSent()
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can delete last question from a report when there are several`() {
          val expectedResponse = getResource("/questions-with-responses/delete-response.json")
          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, true)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can delete all questions from a report`() {
          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            whatChanged = WhatChanged.QUESTIONS,
          )

          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              true,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            whatChanged = WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can delete a question from a report first created in NOMIS`() {
          val nomisReportWithQuestionsAndResponses = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              source = InformationSource.NOMIS,
              generateQuestions = 2,
              generateResponses = 2,
            ),
          )

          webTestClient.delete().uri("/incident-reports/${nomisReportWithQuestionsAndResponses.id}/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk

          val updatedNomisReportWithQuestionsAndResponses = reportRepository.findOneByReportReference("11124147")!!.toDtoBasic()
          assertThat(updatedNomisReportWithQuestionsAndResponses.createdInNomis).isTrue()
          assertThat(updatedNomisReportWithQuestionsAndResponses.lastModifiedInNomis).isFalse()
        }
      }
    }
  }
}

data class InvalidRequestTestCase(
  val name: String,
  val request: String,
  val expectedErrorText: String? = null,
)

data class NullablePropertyTestCase(
  val field: String,
  val validValue: String,
  val unchangedValue: String,
) {
  data class ValidRequestTestCase(
    val name: String,
    val request: String,
    val expectedFieldValue: String?,
  )

  fun makeValidRequestTests(): List<ValidRequestTestCase> = listOf(
    ValidRequestTestCase(
      "$field with not value provided",
      // language=json
      "{}",
      unchangedValue,
    ),
    ValidRequestTestCase(
      "$field with null value",
      // language=json
      """
      {"$field": null}
      """,
      null,
    ),
    ValidRequestTestCase(
      "$field with value provided",
      // language=json
      """
      {"$field": "$validValue"}
      """,
      validValue,
    ),
  )
}
