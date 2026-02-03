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
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.util.Optional
import java.util.UUID

@DisplayName("Report resource")
class ReportResourceTest : SqsIntegrationTestBase() {

  @Autowired
  lateinit var reportRepository: ReportRepository

  lateinit var existingReport: Report

  private fun assertThatReportWasModified(id: UUID) {
    webTestClient.get().uri("/incident-reports/$id")
      .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
      .header("Content-Type", "application/json")
      .exchange()
      .expectBody().json(
        // language=json
        """
          {
            "lastModifiedInNomis": false,
            "modifiedAt": "2023-12-05T12:34:56.123456",
            "modifiedBy": "request-user"
          }
          """,
        JsonCompareMode.LENIENT,
      )
  }

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()

    existingReport = reportRepository.save(
      buildReport(
        reportReference = "11124143",
        reportTime = now,
        lastUserAction = UserAction.REQUEST_NOT_REPORTABLE,
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
          "reference=",
          "reference=IR1234",
          "reference=123456789012345678901234567890",
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
            JsonCompareMode.STRICT,
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
                "incidentDateAndTime": "2023-12-05T11:34:56.123456"
              }],
              "number": 0,
              "size": 20,
              "numberOfElements": 1,
              "totalElements": 1,
              "totalPages": 1,
              "sort": ["incidentDateAndTime,DESC"]
            }""",
            JsonCompareMode.LENIENT,
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
                  status = if (fromDps) Status.DRAFT else Status.AWAITING_REVIEW,
                  source = if (fromDps) InformationSource.DPS else InformationSource.NOMIS,
                  type = if (index < 3) Type.FIND_6 else Type.FIRE_1,
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
                    "incidentDateAndTime": "2023-12-05T11:34:56.123456"
                  },
                  {
                    "reportReference": "11017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56.123456"
                  },
                  {
                    "reportReference": "11006603",
                    "incidentDateAndTime": "2023-12-03T11:34:56.123456"
                  },
                  {
                    "reportReference": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56.123456"
                  },
                  {
                    "reportReference": "31934",
                    "incidentDateAndTime": "2023-12-01T11:34:56.123456"
                  }
                ],
                "number": 0,
                "size": 20,
                "numberOfElements": 5,
                "totalElements": 5,
                "totalPages": 1,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              JsonCompareMode.LENIENT,
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
                    "incidentDateAndTime": "2023-12-05T11:34:56.123456"
                  },
                  {
                    "reportReference": "11017203",
                    "incidentDateAndTime": "2023-12-04T11:34:56.123456"
                  }
                ],
                "number": 0,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              JsonCompareMode.LENIENT,
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
                    "incidentDateAndTime": "2023-12-03T11:34:56.123456"
                  },
                  {
                    "reportReference": "94728",
                    "incidentDateAndTime": "2023-12-02T11:34:56.123456"
                  }
                ],
                "number": 1,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["incidentDateAndTime,DESC"]
              }""",
              JsonCompareMode.LENIENT,
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
            "reportReference,ASC" to listOf("31934", "94728", "11006603", "11017203", "11124143"),
            "reportReference,DESC" to listOf("11124143", "11017203", "11006603", "94728", "31934"),
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
              JsonCompareMode.LENIENT,
            ).jsonPath("content[*].reportReference").value<List<String>> {
              assertThat(it).isEqualTo(expectedReportReferences)
            }
        }

        @ParameterizedTest(name = "can filter reports by `{0}`")
        @CsvSource(
          value = [
            "''                                                          | 5",
            "reference=11006603                                          | 1",
            "reference=11006603&location=MDI                             | 0",
            "reference=11006604                                          | 0",
            "location=                                                   | 5",
            "location=MDI                                                | 3",
            "location=LEI,MDI                                            | 5",
            "location=MDI&location=LEI                                   | 5",
            "location=BXI                                                | 0",
            "status=                                                     | 5",
            "status=DRAFT                                                | 3",
            "status=AWAITING_REVIEW                                      | 2",
            "status=AWAITING_REVIEW,DRAFT                                | 5",
            "status=AWAITING_REVIEW&status=DRAFT                         | 5",
            "status=AWAITING_REVIEW,DRAFT&source=DPS                     | 3",
            "status=CLOSED                                               | 0",
            "source=DPS                                                  | 3",
            "source=NOMIS                                                | 2",
            "source=NOMIS&location=MDI                                   | 2",
            "source=DPS&location=MDI                                     | 1",
            "type=ASSAULT_5                                              | 0",
            "type=FIRE_1                                                 | 1",
            "type=FIND_6,FIRE_1                                          | 5",
            "type=FIND_6&type=FIRE_1                                     | 5",
            "type=FIRE_1&location=LEI                                    | 0",
            "type=FIRE_1&location=MDI                                    | 1",
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
            "location=LEI,MDI                                            | 5",
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
              "type": "FIND_6",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "latestUserAction": "REQUEST_NOT_REPORTABLE"
            }
            """,
            JsonCompareMode.STRICT,
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
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "questions": [],
              "history": [],
              "incidentTypeHistory":[],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "latestUserAction": "REQUEST_NOT_REPORTABLE",
              "staffInvolvementDone": true,
              "prisonerInvolvementDone": true,
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "nomisStatus": null,
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null
            }
            """,
            JsonCompareMode.STRICT,
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
              "type": "FIND_6",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "latestUserAction": "REQUEST_NOT_REPORTABLE"
            }
            """,
            JsonCompareMode.STRICT,
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
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "questions": [],
              "history": [],
              "incidentTypeHistory": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "latestUserAction": null,
              "staffInvolvementDone": true,
              "prisonerInvolvementDone": true,
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "nomisStatus": null,
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "latestUserAction": "REQUEST_NOT_REPORTABLE"
            }
            """,
            JsonCompareMode.STRICT,
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
      type = Type.SELF_HARM_1,
      location = "MDI",
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
      fun `cannot create a report with an inactive type`() {
        webTestClient.post().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(createReportRequest.copy(type = Type.ASSAULT_1).toJson())
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Inactive incident type ASSAULT_1")
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
              "type": "SELF_HARM_1",
              "nomisType": "SELF_HARM",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "An incident occurred",
              "description": "Longer explanation of incident",
              "descriptionAddendums": [],
              "questions": [],
              "history": [],
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user"
                }
              ],
              "staffInvolved": [],
              "prisonersInvolved": [],
              "correctionRequests": [],
              "staffInvolvementDone": false,
              "prisonerInvolvementDone": false,
              "reportedBy": "request-user",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "nomisStatus": null,
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatDomainEventWasSent(
          "incident.report.created",
          null,
          "MDI",
          WhatChanged.ANYTHING,
        )
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}")
  @Nested
  inner class UpdateReport {
    private lateinit var url: String
    lateinit var duplicatedExistingReport: Report

    @BeforeEach
    fun setUp() {
      url = "/incident-reports/${existingReport.id}"

      duplicatedExistingReport = reportRepository.save(
        buildReport(
          reportReference = "11124144",
          reportTime = now,
        ),
      )
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
          .bodyValue("{}")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FIND_6",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "latestUserAction": "REQUEST_NOT_REPORTABLE"
            }
            """,
            JsonCompareMode.STRICT,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can update all incident report fields`() {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = now.minusHours(2),
          location = "LEI",
          title = "Updated report 11124143",
          description = "Updated incident report of type find of illicit items",
          duplicatedReportId = Optional.of(duplicatedExistingReport.id!!),
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
              "type": "FIND_6",
              "incidentDateAndTime": "2023-12-05T10:34:56.123456",
              "location": "LEI",
              "title": "Updated report 11124143",
              "description": "Updated incident report of type find of illicit items",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": "${duplicatedExistingReport.id}",
              "latestUserAction": "REQUEST_NOT_REPORTABLE"
            }
            """,
            JsonCompareMode.STRICT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          "LEI",
          WhatChanged.BASIC_REPORT,
        )
      }

      @ParameterizedTest(name = "can update `{0}` of an incident report")
      @ValueSource(strings = ["incidentDateAndTime", "location", "title", "description", "duplicatedReportId"])
      fun `can update an incident report field`(fieldName: String) {
        val updateReportRequest = UpdateReportRequest(
          incidentDateAndTime = if (fieldName == "incidentDateAndTime") now.minusHours(2) else null,
          location = if (fieldName == "location") "LEI" else null,
          title = if (fieldName == "title") "Updated report 11124143" else null,
          description = if (fieldName == "description") {
            "Updated incident report of type find of illicit items"
          } else {
            null
          },
          duplicatedReportId = if (fieldName == "duplicatedReportId") {
            Optional.of(duplicatedExistingReport.id!!)
          } else {
            null
          },
        )
        val expectedIncidentDateAndTime = if (fieldName == "incidentDateAndTime") {
          "2023-12-05T10:34:56.123456"
        } else {
          "2023-12-05T11:34:56.123456"
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
          "Updated incident report of type find of illicit items"
        } else {
          "A new incident created in the new service of type find of illicit items"
        }
        val expectedDuplicatedReportId = if (fieldName == "duplicatedReportId") {
          "${duplicatedExistingReport.id}"
        } else {
          null
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
              "type": "FIND_6",
              "incidentDateAndTime": "$expectedIncidentDateAndTime",
              "location": "$expectedLocation",
              "title": "$expectedTitle",
              "description": "$expectedDescription",
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": $expectedDuplicatedReportId,
              "latestUserAction":"REQUEST_NOT_REPORTABLE"
            }
            """,
            JsonCompareMode.STRICT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          updateReportRequest.location ?: "MDI",
          WhatChanged.BASIC_REPORT,
        )
      }

      @ParameterizedTest(name = "can update involvement flags: {0}")
      @ValueSource(strings = ["staff", "prisoner", "both"])
      fun `can update involvement flags`(flag: String) {
        val newReportId = reportRepository.save(
          buildReport(
            reportReference = "11124149",
            reportTime = now,
          ).apply {
            staffInvolvementDone = false
            prisonerInvolvementDone = false
          },
        ).id!!

        val updateReportRequest = UpdateReportRequest(
          staffInvolvementDone = if (flag == "staff" || flag == "both") {
            true
          } else {
            null
          },
          prisonerInvolvementDone = if (flag == "prisoner" || flag == "both") {
            true
          } else {
            null
          },
        )
        webTestClient.patch().uri("/incident-reports/$newReportId")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(updateReportRequest.toJson())
          .exchange()
          .expectStatus().isOk

        val updatedReport = reportRepository.findById(newReportId).get()
        when (flag) {
          "staff" -> {
            assertThat(updatedReport.staffInvolvementDone).isTrue()
            assertThat(updatedReport.prisonerInvolvementDone).isFalse()
          }
          "prisoner" -> {
            assertThat(updatedReport.staffInvolvementDone).isFalse()
            assertThat(updatedReport.prisonerInvolvementDone).isTrue()
          }
          "both" -> {
            assertThat(updatedReport.staffInvolvementDone).isTrue()
            assertThat(updatedReport.prisonerInvolvementDone).isTrue()
          }
        }

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124149",
          "MDI",
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
            JsonCompareMode.LENIENT,
          )
      }
    }
  }

  @DisplayName("PATCH /incident-reports/{id}/status")
  @Nested
  inner class ChangeStatus {
    private lateinit var url: String

    // language=json
    private fun validPayload(newStatus: Status = Status.AWAITING_REVIEW) = """
      {"newStatus": "${newStatus.name}"}
    """

    private val validPayloadWithCorrection = """
      {
        "newStatus": "AWAITING_REVIEW",
        "correctionRequest": {
          "descriptionOfChange": "This report need to be removed as it is not reportable",
          "userType": "REPORTING_OFFICER",
          "userAction": "REQUEST_NOT_REPORTABLE"
        }
      }
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
        webTestClient.patch().uri(url).bodyValue(validPayload()),
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
          .bodyValue(validPayload())
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
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "DRAFT",
              "nomisStatus": null,
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "USER1"
                }
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can change status of a report preserving history`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "AWAITING_REVIEW",
              "nomisStatus": "AWAN",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "USER1"
                },
                {
                  "status": "AWAITING_REVIEW",
                  "nomisStatus": "AWAN",
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user"
                }
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          "MDI",
          WhatChanged.STATUS,
        )
      }

      @Test
      fun `can change status of a report and include correction request`() {
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayloadWithCorrection)
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "latestUserAction": "REQUEST_NOT_REPORTABLE",
              "correctionRequests": [
                 {
                    "descriptionOfChange": "This report need to be removed as it is not reportable",
                    "correctionRequestedBy": "request-user",
                    "correctionRequestedAt": "2023-12-05T12:34:56.123456",
                    "userAction": "REQUEST_NOT_REPORTABLE",
                    "originalReportReference": null,
                    "userType": "REPORTING_OFFICER"
                  }
              ],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "AWAITING_REVIEW",
              "nomisStatus": "AWAN",
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "USER1"
                },
                {
                  "status": "AWAITING_REVIEW",
                  "nomisStatus": "AWAN",
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user"
                }
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          "MDI",
          WhatChanged.STATUS,
        )

        // the DW marks the report as not reportable
        webTestClient.patch().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validPayload(Status.NOT_REPORTABLE))
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${existingReport.id}",
              "reportReference": "11124143",
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:34:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124143",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "latestUserAction": null,
              "correctionRequests": [
                 {
                    "descriptionOfChange": "This report need to be removed as it is not reportable",
                    "correctionRequestedBy": "request-user",
                    "correctionRequestedAt": "2023-12-05T12:34:56.123456",
                    "userAction": "REQUEST_NOT_REPORTABLE",
                    "originalReportReference": null,
                    "userType": "REPORTING_OFFICER"
                  }
              ],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:34:56.123456",
              "status": "NOT_REPORTABLE",
              "nomisStatus": null,
              "createdAt": "2023-12-05T12:34:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "historyOfStatuses": [
                {
                  "status": "DRAFT",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "USER1"
                },
                {
                  "status": "AWAITING_REVIEW",
                  "nomisStatus": "AWAN",
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user"
                },
                 {
                  "status": "NOT_REPORTABLE",
                  "nomisStatus": null,
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user"
                }
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124143",
          "MDI",
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
          .bodyValue(validPayload())
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "id": "${nomisReport.id}",
              "status": "AWAITING_REVIEW",
              "createdInNomis": true,
              "lastModifiedInNomis": false
            }
            """,
            JsonCompareMode.LENIENT,
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
      {"newType": "FIRE_1"}
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
              {"type": "FIRE_1"}
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
              {"newType": "ASSAULT_4"}
            """,
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("developerMessage").value<String> {
            assertThat(it).contains("Inactive incident type ASSAULT_4")
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
            status = Status.AWAITING_REVIEW,
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
              {"newType": "FIND_6"}
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
              "type": "FIND_6",
              "nomisType": "FIND0422",
              "incidentDateAndTime": "2023-12-05T11:31:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124146",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56.123456",
              "status": "AWAITING_REVIEW",
              "nomisStatus": "AWAN",
              "createdAt": "2023-12-05T12:31:56.123456",
              "modifiedAt": "2023-12-05T12:31:56.123456",
              "modifiedBy": "USER1",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "history": [],
              "questions": [
                {
                  "code": "1",
                  "question": "Question #1",
                  "additionalInformation": "Explanation #1",
                  "responses": [
                    {
                      "code": "1-1",
                      "response": "Response #1",
                      "label": "Label #1",
                      "responseDate": "2023-12-04",
                      "additionalInformation": "Prose #1",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56.123456"
                    },
                    {
                      "code": "1-2",
                      "response": "Response #2",
                      "label": "Label #2",
                      "responseDate": "2023-12-03",
                      "additionalInformation": "Prose #2",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56.123456"
                    }
                  ]
                },
                {
                  "code": "2",
                  "question": "Question #2",
                  "additionalInformation": "Explanation #2",
                  "responses": [
                    {
                      "code": "2-1",
                      "response": "Response #1",
                      "label": "Label #1",
                      "responseDate": "2023-12-04",
                      "additionalInformation": "Prose #1",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56.123456"
                    },
                    {
                      "code": "2-2",
                      "response": "Response #2",
                      "label": "Label #2",
                      "responseDate": "2023-12-03",
                      "additionalInformation": "Prose #2",
                      "recordedBy": "some-user",
                      "recordedAt": "2023-12-05T12:31:56.123456"
                    }
                  ]
                }
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatNoDomainEventsWereSent()
      }

      @Test
      fun `can change type of a report creating history when there was none`() {
        val reportWithQuestions = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_REVIEW,
            generateQuestions = 2,
            generateResponses = 2,
            generateStaffInvolvement = 1,
            generatePrisonerInvolvement = 1,
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
              "type": "FIRE_1",
              "nomisType": "FIRE",
              "incidentDateAndTime": "2023-12-05T11:31:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124146",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56.123456",
              "status": "AWAITING_REVIEW",
              "nomisStatus": "AWAN",
              "createdAt": "2023-12-05T12:31:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "history": [
                {
                  "type": "FIND_6",
                  "nomisType": "FIND0422",
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user",
                  "questions": [
                    {
                      "code": "1",
                      "question": "Question #1",
                      "additionalInformation": "Explanation #1",
                      "responses": [
                        {
                          "code": "1-1",
                          "response": "Response #1",
                          "label": "Label #1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56.123456"
                        },
                        {
                          "code": "1-2",
                          "response": "Response #2",
                          "label": "Label #2",
                          "responseDate": "2023-12-03",
                          "additionalInformation": "Prose #2",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56.123456"
                        }
                      ]
                    },
                    {
                      "code": "2",
                      "question": "Question #2",
                      "additionalInformation": "Explanation #2",
                      "responses": [
                        {
                          "code": "2-1",
                          "response": "Response #1",
                          "label": "Label #1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56.123456"
                        },
                        {
                          "code": "2-2",
                          "response": "Response #2",
                          "label": "Label #2",
                          "responseDate": "2023-12-03",
                          "additionalInformation": "Prose #2",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56.123456"
                        }
                      ]
                    }
                  ]
                }
              ],
              "questions": [],
              "historyOfStatuses": [
                {
                  "status": "AWAITING_REVIEW",
                  "nomisStatus": "AWAN",
                  "changedAt": "2023-12-05T12:31:56.123456",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [
                {
                  "staffUsername": "staff-1",
                  "firstName": "First 1",
                  "lastName": "Last 1",
                  "staffRole": "AUTHORISING_OFFICER",
                  "comment": "Comment #1",
                  "sequence": 0
                }
              ],
              "prisonersInvolved": []
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124146",
          "MDI",
          WhatChanged.TYPE,
        )
      }

      @Test
      fun `can change type of a report preserving history when it already existed`() {
        val reportWithQuestionsAndHistory = reportRepository.save(
          buildReport(
            reportReference = "11124146",
            reportTime = now.minusMinutes(3),
            status = Status.AWAITING_REVIEW,
            generateQuestions = 1,
            generateResponses = 1,
            generateStaffInvolvement = 1,
            generatePrisonerInvolvement = 1,
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
              "type": "FIRE_1",
              "nomisType": "FIRE",
              "incidentDateAndTime": "2023-12-05T11:31:56.123456",
              "location": "MDI",
              "title": "Incident Report 11124146",
              "description": "A new incident created in the new service of type find of illicit items",
              "descriptionAddendums": [],
              "reportedBy": "USER1",
              "reportedAt": "2023-12-05T12:31:56.123456",
              "status": "AWAITING_REVIEW",
              "nomisStatus": "AWAN",
              "createdAt": "2023-12-05T12:31:56.123456",
              "modifiedAt": "2023-12-05T12:34:56.123456",
              "modifiedBy": "request-user",
              "createdInNomis": false,
              "lastModifiedInNomis": false,
              "duplicatedReportId": null,
              "history": [
                {
                  "type": "ASSAULT_5",
                  "nomisType": "ASSAULTS3",
                  "changedAt": "2023-12-05T12:31:56.123456",
                  "changedBy": "some-past-user",
                  "questions": [
                    {
                      "code": "1-1",
                      "question": "Historical question #1-1",
                      "additionalInformation": "Explanation #1 in history #1",
                      "responses": [
                        {
                          "code": "1-1-1",
                          "response": "Historical response #1-1-1",
                          "label": "Historical label #1-1-1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1 in history #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56.123456"
                        }
                      ]
                    }
                  ]
                },
                {
                  "type": "FIND_6",
                  "nomisType": "FIND0422",
                  "changedAt": "2023-12-05T12:34:56.123456",
                  "changedBy": "request-user",
                  "questions": [
                    {
                      "code": "1",
                      "question": "Question #1",
                      "additionalInformation": "Explanation #1",
                      "responses": [
                        {
                          "code": "1-1",
                          "response": "Response #1",
                          "label": "Label #1",
                          "responseDate": "2023-12-04",
                          "additionalInformation": "Prose #1",
                          "recordedBy": "some-user",
                          "recordedAt": "2023-12-05T12:31:56.123456"
                        }
                      ]
                    }
                  ]
                }
              ],
              "questions": [],
              "historyOfStatuses": [
                {
                  "status": "AWAITING_REVIEW",
                  "nomisStatus": "AWAN",
                  "changedAt": "2023-12-05T12:31:56.123456",
                  "changedBy": "USER1"
                }
              ],
              "staffInvolved": [
                {
                  "staffUsername": "staff-1",
                  "firstName": "First 1",
                  "lastName": "Last 1",
                  "staffRole": "AUTHORISING_OFFICER",
                  "comment": "Comment #1",
                  "sequence": 0
                }
              ],
              "prisonersInvolved": []
            }
            """,
            JsonCompareMode.LENIENT,
          )

        assertThatDomainEventWasSent(
          "incident.report.amended",
          "11124146",
          "MDI",
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
              "type": "FIRE_1",
              "createdInNomis": true,
              "lastModifiedInNomis": false
            }
            """,
            JsonCompareMode.LENIENT,
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
        "DELETE_INCIDENT_REPORTS",
        "write",
      )
    }

    @DisplayName("validates requests")
    @Nested
    inner class Validation {
      @Test
      fun `cannot delete a report by ID if it is not found`() {
        webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555")
          .headers(setAuthorisation(roles = listOf("ROLE_DELETE_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isNotFound

        assertThatNoDomainEventsWereSent()
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {
      @Test
      fun `can delete a report by ID`() {
        val reportId = existingReport.id!!

        webTestClient.delete().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_DELETE_INCIDENT_REPORTS"), scopes = listOf("write")))
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
            JsonCompareMode.LENIENT,
          )

        assertThat(reportRepository.findOneEagerlyById(reportId)).isNull()

        assertThatDomainEventWasSent(
          "incident.report.deleted",
          "11124143",
          "MDI",
          WhatChanged.ANYTHING,
        )
      }
    }
  }

  @Nested
  abstract inner class RelatedObjects(
    val urlSuffix: String,
    val whatChanged: WhatChanged,
  ) {
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
          generateDescriptionAddendums = 2,
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
              JsonCompareMode.STRICT,
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReport.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124143",
            "MDI",
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            whatChanged,
          )
        }

        @Test
        fun `can add object to a report first created in NOMIS`() {
          val nomisReportWithRelatedObjects = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              source = InformationSource.NOMIS,
              generateDescriptionAddendums = 2,
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

          val updatedNomisReportWithRelatedObjects = reportRepository.findOneByReportReference(
            "11124147",
          )!!.toDtoBasic()
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            whatChanged,
          )
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            whatChanged,
          )
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
                  .expectBody().jsonPath("[0].${testCase.field}").isEqualTo(
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    expectedFieldValue,
                  )
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
            generateDescriptionAddendums = 2,
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            whatChanged,
          )
        }

        @Test
        fun `can delete second related object from a report`() {
          val expectedResponse = getResource("/related-objects/$urlSuffix/delete-response-second.json")
          webTestClient.delete().uri(urlForSecondRelatedObject)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithRelatedObjects.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            whatChanged,
          )
        }

        @Test
        fun `can delete related object from a report first created in NOMIS`() {
          val nomisReportWithRelatedObjects = reportRepository.save(
            buildReport(
              reportReference = "11124147",
              reportTime = now,
              source = InformationSource.NOMIS,
              generateDescriptionAddendums = 2,
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

          val updatedNomisReportWithRelatedObjects = reportRepository.findOneByReportReference(
            "11124147",
          )!!.toDtoBasic()
          assertThat(updatedNomisReportWithRelatedObjects.createdInNomis).isTrue()
          assertThat(updatedNomisReportWithRelatedObjects.lastModifiedInNomis).isFalse()
        }
      }
    }
  }

  @DisplayName("Description addendums")
  @Nested
  inner class DescriptionAddendums : RelatedObjects("description-addendums", WhatChanged.DESCRIPTION_ADDENDUMS) {
    @DisplayName("GET /incident-reports/{reportId}/description-addendums")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/description-addendums")
    @Nested
    inner class AddObject :
      RelatedObjects.AddObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "short username",
            getResource("/related-objects/description-addendums/add-request-short-username.json"),
            "addDescriptionAddendum.createdBy: size must be between 3 and 120",
          ),
          InvalidRequestTestCase(
            "empty name",
            getResource("/related-objects/description-addendums/add-request-empty-name.json"),
            "addDescriptionAddendum.firstName: size must be between 1 and 255",
          ),
          InvalidRequestTestCase(
            "long name",
            getResource("/related-objects/description-addendums/add-request-long-name.json"),
            "addDescriptionAddendum.lastName: size must be between 1 and 255",
          ),
        ),
      ) {
      @Test
      fun `can add description addendum without createdBy field`() {
        val validRequest = getResource("/related-objects/description-addendums/add-request-no-username.json")
        webTestClient.post().uri(urlWithoutRelatedObjects)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """
            [
              {
                "createdBy": "request-user"
              }
            ]
            """,
            JsonCompareMode.LENIENT,
          )
      }

      @Test
      fun `can add description addendum without createdAt field`() {
        val validRequest = getResource("/related-objects/description-addendums/add-request-no-date.json")
        webTestClient.post().uri(urlWithoutRelatedObjects)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isCreated
          .expectBody().json(
            // language=json
            """
            [
              {
                "createdAt": "2023-12-05T12:34:56.123456"
              }
            ]
            """,
            JsonCompareMode.LENIENT,
          )
      }
    }

    @DisplayName("PATCH /incident-reports/{reportId}/description-addendums/{index}")
    @Nested
    inner class UpdateObject :
      RelatedObjects.UpdateObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "long username",
            getResource("/related-objects/description-addendums/update-request-long-username.json"),
            "updateDescriptionAddendum.createdBy: size must be between 3 and 120",
          ),
          InvalidRequestTestCase(
            "empty text",
            getResource("/related-objects/description-addendums/update-request-empty-text.json"),
            "updateDescriptionAddendum.text: size must be between 1 and",
          ),
        ),
      )

    @DisplayName("DELETE /incident-reports/{reportId}/description-addendums/{index}")
    @Nested
    inner class RemoveObject : RelatedObjects.RemoveObject()
  }

  @DisplayName("Staff involvement")
  @Nested
  inner class StaffInvolvement : RelatedObjects("staff-involved", WhatChanged.STAFF_INVOLVED) {
    @DisplayName("GET /incident-reports/{reportId}/staff-involved")
    @Nested
    inner class ListObjects : RelatedObjects.ListObjects()

    @DisplayName("POST /incident-reports/{reportId}/staff-involved")
    @Nested
    inner class AddObject :
      RelatedObjects.AddObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "short staff username",
            getResource("/related-objects/staff-involved/add-request-short-username.json"),
            "addStaffInvolvement.staffUsername: size must be between 3 and 120",
          ),
          InvalidRequestTestCase(
            "empty surname",
            getResource("/related-objects/staff-involved/add-request-empty-surname.json"),
            "addStaffInvolvement.lastName: size must be between 1 and 255",
          ),
          InvalidRequestTestCase(
            "long surname",
            getResource("/related-objects/staff-involved/add-request-long-surname.json"),
            "addStaffInvolvement.lastName: size must be between 1 and 255",
          ),
        ),
      ) {
      @Test
      fun `automatically flags staff involvements as done`() {
        val newReportId = reportRepository.save(
          buildReport(
            reportReference = "11124149",
            reportTime = now,
          ).apply {
            staffInvolvementDone = false
            prisonerInvolvementDone = false
          },
        ).id!!

        webTestClient.post().uri("/incident-reports/$newReportId/$urlSuffix")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isCreated

        val updatedReport = reportRepository.findById(newReportId).get()
        assertThat(updatedReport.staffInvolvementDone).isTrue()
        assertThat(updatedReport.prisonerInvolvementDone).isFalse()
      }
    }

    @DisplayName("PATCH /incident-reports/{reportId}/staff-involved/{index}")
    @Nested
    inner class UpdateObject :
      RelatedObjects.UpdateObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "short staff username",
            getResource("/related-objects/staff-involved/update-request-short-username.json"),
            "updateStaffInvolvement.staffUsername: size must be between 3 and 120",
          ),
          InvalidRequestTestCase(
            "empty name",
            getResource("/related-objects/staff-involved/update-request-empty-name.json"),
            "updateStaffInvolvement.firstName: size must be between 1 and 255",
          ),
          InvalidRequestTestCase(
            "long name",
            getResource("/related-objects/staff-involved/update-request-long-name.json"),
            "updateStaffInvolvement.firstName: size must be between 1 and 255",
          ),
        ),
        nullablePropertyRequests = listOf(
          NullablePropertyTestCase(
            field = "staffUsername",
            validValue = "staff-7",
            unchangedValue = "staff-1",
          ),
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
    inner class AddObject :
      RelatedObjects.AddObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "empty name",
            getResource("/related-objects/prisoners-involved/add-request-empty-name.json"),
            "addPrisonerInvolvement.firstName: size must be between 1 and 255",
          ),
          InvalidRequestTestCase(
            "long name",
            getResource("/related-objects/prisoners-involved/add-request-long-name.json"),
            "addPrisonerInvolvement.firstName: size must be between 1 and 255",
          ),
        ),
      ) {
      @Test
      fun `automatically flags prisoner involvements as done`() {
        val newReportId = reportRepository.save(
          buildReport(
            reportReference = "11124149",
            reportTime = now,
          ).apply {
            staffInvolvementDone = false
            prisonerInvolvementDone = false
          },
        ).id!!

        webTestClient.post().uri("/incident-reports/$newReportId/$urlSuffix")
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
          .header("Content-Type", "application/json")
          .bodyValue(validRequest)
          .exchange()
          .expectStatus().isCreated

        val updatedReport = reportRepository.findById(newReportId).get()
        assertThat(updatedReport.staffInvolvementDone).isFalse()
        assertThat(updatedReport.prisonerInvolvementDone).isTrue()
      }
    }

    @DisplayName("PATCH /incident-reports/{reportId}/prisoners-involved/{index}")
    @Nested
    inner class UpdateObject :
      RelatedObjects.UpdateObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "empty surname",
            getResource("/related-objects/prisoners-involved/update-request-empty-surname.json"),
            "updatePrisonerInvolvement.lastName: size must be between 1 and 255",
          ),
          InvalidRequestTestCase(
            "long surname",
            getResource("/related-objects/prisoners-involved/update-request-long-surname.json"),
            "updatePrisonerInvolvement.lastName: size must be between 1 and 255",
          ),
        ),
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
    inner class AddObject :
      RelatedObjects.AddObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "empty description of change",
            getResource("/related-objects/correction-requests/add-request-empty-description.json"),
            "addCorrectionRequest.descriptionOfChange: size must be between 1 and",
          ),
          InvalidRequestTestCase(
            "short location of change",
            getResource("/related-objects/correction-requests/add-request-short-location.json"),
            "addCorrectionRequest.location: size must be between 2 and 20",
          ),
          InvalidRequestTestCase(
            "long location of change",
            getResource("/related-objects/correction-requests/add-request-long-location.json"),
            "addCorrectionRequest.location: size must be between 2 and 20",
          ),
        ),
      )

    @Test
    fun `can add correction request without userAction, userType, or originalReportReference`() {
      val validRequest = getResource("/related-objects/correction-requests/add-request-with-all-parameters.json")
      webTestClient.post().uri(urlWithoutRelatedObjects)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
        .header("Content-Type", "application/json")
        .bodyValue(validRequest)
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(
          // language=json
          """
            [
              {
                "descriptionOfChange": "Found to be a duplicate",
                "correctionRequestedBy": "request-user",
                "correctionRequestedAt": "2023-12-05T12:34:56.123456",
                "location": "MDI",
                "userAction": "MARK_DUPLICATE",
                "originalReportReference": "12345678",
                "userType": "DATA_WARDEN"
              }
            ]
            """,
          JsonCompareMode.LENIENT,
        )

      // assert lastUserAction on the report reflects the most recent correction request
      val updated = reportRepository.findOneByReportReference("11124143")!!
      assertThat(updated.lastUserAction).isEqualTo(UserAction.MARK_DUPLICATE)
    }

    @DisplayName("PATCH /incident-reports/{reportId}/correction-requests/{index}")
    @Nested
    inner class UpdateObject :
      RelatedObjects.UpdateObject(
        invalidRequests = listOf(
          InvalidRequestTestCase(
            "empty description of change",
            getResource("/related-objects/correction-requests/update-request-empty-description.json"),
            "updateCorrectionRequest.descriptionOfChange: size must be between 1 and",
          ),
          InvalidRequestTestCase(
            "short location of change",
            getResource("/related-objects/correction-requests/update-request-short-location.json"),
            "updateCorrectionRequest.location: size must be between 2 and 20",
          ),
          InvalidRequestTestCase(
            "long location of change",
            getResource("/related-objects/correction-requests/update-request-long-location.json"),
            "updateCorrectionRequest.location: size must be between 2 and 20",
          ),
        ),
      )

    @Test
    fun `can update correction request with all parameters`() {
      val validRequest = getResource("/related-objects/correction-requests/update-request-all-parameters.json")
      webTestClient.post().uri(urlWithoutRelatedObjects)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
        .header("Content-Type", "application/json")
        .bodyValue(validRequest)
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(
          // language=json
          """
            [
              {
                "descriptionOfChange": "This was found to be a duplicate",
                "location": "LEI",
                "userAction": "MARK_DUPLICATE",
                "originalReportReference": "12345678",
                "userType": "DATA_WARDEN"
              }
            ]
            """,
          JsonCompareMode.LENIENT,
        )
    }

    @Test
    fun `can update correction request with nullable parameters as null`() {
      val validRequest = getResource("/related-objects/correction-requests/update-request-null-extra-parameters.json")
      webTestClient.post().uri(urlWithoutRelatedObjects)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
        .header("Content-Type", "application/json")
        .bodyValue(validRequest)
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(
          // language=json
          """
            [
              {
                "descriptionOfChange": "Description needs to mention prisoner numbers",
                "location": "LEI",
                "userAction": null,
                "originalReportReference": null,
                "userType": null
              }
            ]
            """,
          JsonCompareMode.LENIENT,
        )

      // assert lastUserAction is null when the most recent correction request has no userAction
      val updated = reportRepository.findOneByReportReference("11124143")!!
      assertThat(updated.lastUserAction).isNull()
    }

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
              JsonCompareMode.STRICT,
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
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
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

    @DisplayName("PUT /incident-reports/{reportId}/questions")
    @Nested
    inner class AddOrUpdateQuestions {
      private val validRequest = getResource("/questions-with-responses/add-request-with-responses.json")

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.put().uri(urlWithQuestionsAndResponses).bodyValue(validRequest),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot add question and responses to a report if it is not found`() {
          webTestClient.put().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions")
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
              "addOrUpdateQuestionsWithResponses.requests: size must be between 1 and",
            ),
            InvalidRequestTestCase(
              "long code",
              getResource("/questions-with-responses/add-request-long-code.json"),
              "addOrUpdateQuestionsWithResponses.requests[0].code: size must be between 1 and 60",
            ),
            InvalidRequestTestCase(
              "empty question",
              getResource("/questions-with-responses/add-request-empty-question.json"),
              "addOrUpdateQuestionsWithResponses.requests[0].question: size must be between 1 and",
            ),
            InvalidRequestTestCase(
              "empty response",
              getResource("/questions-with-responses/add-request-empty-response.json"),
              "addOrUpdateQuestionsWithResponses.requests[0].responses[1].response: size must be between 1 and",
            ),
          )
            .map { (name, request, expectedErrorText) ->
              DynamicTest.dynamicTest(name) {
                webTestClient.put().uri(urlWithoutQuestions)
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
          webTestClient.put().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(
                it,
              ).contains("addOrUpdateQuestionsWithResponses.requests[0].responses: size must be between 1 and")
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
          webTestClient.put().uri(urlWithoutQuestions)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReport.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124143",
            "MDI",
            WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses to report with existing questions`() {
          val expectedResponse = getResource("/questions-with-responses/add-response-with-responses.json")
          webTestClient.put().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add question with responses that have nullable fields`() {
          val validRequestWithNulls =
            getResource("/questions-with-responses/add-request-with-responses-and-null-fields.json")
          val expectedResponse =
            getResource("/questions-with-responses/add-response-with-responses-and-null-fields.json")
          webTestClient.put().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequestWithNulls)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can update existing question with responses`() {
          val validUpdateRequest = getResource("/questions-with-responses/update-request-with-responses.json")
          val expectedResponse = getResource("/questions-with-responses/update-response-with-responses.json")
          webTestClient.put().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validUpdateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add multiple questions with responses in one go`() {
          val validRequestWith3Questions =
            getResource("/questions-with-responses/add-request-3-questions-with-responses.json")
          val expectedResponse = getResource("/questions-with-responses/add-response-3-questions-with-responses.json")
          webTestClient.put().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequestWith3Questions)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
          )
        }

        @Test
        fun `can add and update questions with one request containing several payloads`() {
          val validAddAndUpdateRequest =
            getResource("/questions-with-responses/add-and-update-request-with-responses.json")
          val expectedResponse = getResource("/questions-with-responses/add-and-update-response-with-responses.json")
          webTestClient.put().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validAddAndUpdateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
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

          webTestClient.put().uri("/incident-reports/${nomisReportWithQuestionsAndResponses.id}/questions")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .bodyValue(validRequest)
            .exchange()
            .expectStatus().isOk

          val updatedNomisReportWithQuestionsAndResponses = reportRepository.findOneByReportReference(
            "11124147",
          )!!.toDtoBasic()
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
          webTestClient.delete().uri("$urlWithQuestionsAndResponses?code=1"),
          "MAINTAIN_INCIDENT_REPORTS",
          "write",
        )
      }

      @DisplayName("validates requests")
      @Nested
      inner class Validation {
        @Test
        fun `cannot delete zero questions from a report`() {
          webTestClient.delete().uri(urlWithQuestionsAndResponses)
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("deleteQuestionsAndResponses.code: size must be between 1")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete question with blank codes`() {
          webTestClient.delete().uri("$urlWithQuestionsAndResponses?code=&code=")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("size must be between 1")
            }

          assertThatNoDomainEventsWereSent()
        }

        @Test
        fun `cannot delete question from a report when report is not found`() {
          webTestClient.delete().uri("/incident-reports/11111111-2222-3333-4444-555555555555/questions?code=1")
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
        fun `cannot delete question from a report when the code is not found`() {
          webTestClient.delete().uri("$urlWithoutQuestions?code=1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("Questions codes not found: 1")
            }

          assertThatNoDomainEventsWereSent()

          val remainingQuestionCodes = reportRepository.findOneEagerlyById(existingReport.id!!)!!
            .questions
            .map { it.code }
          assertThat(remainingQuestionCodes).isEmpty()
        }

        @Test
        fun `cannot delete any questions from a report when at least one code is not found`() {
          webTestClient.delete().uri("$urlWithQuestionsAndResponses?code=1&code=3&code=4")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isNotFound
            .expectBody().jsonPath("developerMessage").value<String> {
              assertThat(it).contains("Questions codes not found: 3, 4")
            }

          assertThatNoDomainEventsWereSent()

          val remainingQuestionCodes = reportRepository.findOneEagerlyById(
            existingReportWithQuestionsAndResponses.id!!,
          )!!
            .questions
            .map { it.code }
          assertThat(remainingQuestionCodes).isEqualTo(listOf("1", "2"))
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {
        @Test
        fun `can delete a question from a report`() {
          val expectedResponse = getResource("/questions-with-responses/delete-response.json")
          webTestClient.delete().uri("$urlWithQuestionsAndResponses?code=2")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
          )
        }

        @ParameterizedTest(name = "can delete several questions from a report with {0}")
        @ValueSource(strings = ["code=1,2", "code=1&code=2"])
        fun `can delete several questions from a report`(queryString: String) {
          webTestClient.delete().uri("$urlWithQuestionsAndResponses?$queryString")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              "[]",
              JsonCompareMode.STRICT,
            )

          assertThatReportWasModified(existingReportWithQuestionsAndResponses.id!!)

          assertThatDomainEventWasSent(
            "incident.report.amended",
            "11124146",
            "MDI",
            WhatChanged.QUESTIONS,
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

          webTestClient.delete().uri("/incident-reports/${nomisReportWithQuestionsAndResponses.id}/questions?code=1")
            .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_INCIDENT_REPORTS"), scopes = listOf("write")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody().json(
              // language=json
              """
              [
                {"code": "2"}
              ]
              """,
              JsonCompareMode.LENIENT,
            )

          val updatedNomisReportWithQuestionsAndResponses = reportRepository.findOneByReportReference(
            "11124147",
          )!!.toDtoBasic()
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
    val expectedFieldValue: Any?,
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
