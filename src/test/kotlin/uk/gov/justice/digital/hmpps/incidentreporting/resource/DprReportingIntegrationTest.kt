package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.Clock

@DisplayName("DPR reporting resource tests")
class DprReportingIntegrationTest : SqsIntegrationTestBase() {

  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Value("\${dpr.lib.system.role}")
  lateinit var systemRole: String

  @Autowired
  lateinit var reportRepository: ReportRepository

  @Autowired
  lateinit var eventRepository: EventRepository

  lateinit var existingReport: Report

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    existingReport = reportRepository.save(
      buildReport(
        reportReference = "11124143",
        reportTime = now,
        generateStaffInvolvement = 3,
        generatePrisonerInvolvement = 2,
      ),
    )

    manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RW"))
    manageUsersMockServer.stubLookupUserCaseload("request-user", "LEI", listOf("MDI"))
  }

  @DisplayName("GET /definitions")
  @Nested
  inner class GetDefinitions {
    private val url = "/definitions"

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        systemRole,
      )
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns the definitions of all the reports`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath("$.length()").isEqualTo(5)
          .jsonPath("$[0].authorised").isEqualTo("true")
      }

      @Test
      fun `returns the definitions of all the reports but not authorises as no user in context`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(user = null, roles = listOf(systemRole), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath("$.length()").isEqualTo(5)
          .jsonPath("$[0].authorised").isEqualTo("false")
      }

      @Test
      fun `returns the not auth definitions of the reports`() {
        manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("ANOTHER_USER_ROLE"))

        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(5)
          .jsonPath("$[0].authorised").isEqualTo("false")
      }
    }
  }

  @DisplayName("GET /definitions/incident-report/summary")
  @Nested
  inner class GetDefinitionDetails {
    private val url = "/definitions/incident-report/summary"

    @DisplayName("is secured")
    @Nested
    inner class Security {
      @DisplayName("by role and scope")
      @TestFactory
      fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
        webTestClient.get().uri(url),
        systemRole,
      )

      @Test
      fun `report definition denied when user has incorrect role`() {
        manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("ANOTHER_USER_ROLE"))

        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns the definition of the report`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
              {
              "id": "incident-report",
              "name": "Incident report summary",
              "description": "List of all incidents filtered by dates, types, status and locations (INC0009)",
              "variant": {
                "id": "summary",
                "name": "Incident Report Summary",
                "resourceName": "reports/incident-report/summary",
                "description": "List of all incidents filtered by dates, types, status and locations",
                "printable": true
              }
            }
            """.trimIndent(),
          )
      }
    }
  }

  @DisplayName("GET /reports")
  @Nested
  inner class GetReports {
    @DisplayName("GET /reports/incident-report/summary")
    @Nested
    inner class RunReportIncidentSummary {
      private val url = "/reports/incident-report/summary"

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(url),
          systemRole,
        )

        @Test
        fun `returns 403 when user does not have the role`() {
          manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("ANOTHER_USER_ROLE"))

          webTestClient.get().uri(url)
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report`() {
          webTestClient.get().uri(url)
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
[
  {
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "05/12/2023 11:34",
    "reported_at": "05/12/2023",
    "reported_by": "USER1",
    "title": "Incident Report 11124143",
    "description": "A new incident created in the new service of type find of illicit items",
    "location": "MDI",
    "modified_at": "05/12/2023 12:34"
  }
]
              """.trimIndent(),
            )
        }

        @Test
        fun `returns no data when user does not have the caseload`() {
          manageUsersMockServer.stubLookupUserCaseload("request-user", "BXI", listOf("BXI"))

          webTestClient.get().uri(url)
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(0)
        }
      }
    }

    @DisplayName("GET /reports/incident-report-pecs/summary")
    @Nested
    inner class RunPecsReportIncidentSummary {
      private val url = "/reports/incident-report-pecs/summary"

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(url),
          systemRole,
        )

        @Test
        fun `returns 403 when user does not have the role`() {
          manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__RW"))

          webTestClient.get().uri(url)
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isForbidden
        }
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report if has PECS role`() {
          manageUsersMockServer.stubLookupUsersRoles("request-user", listOf("INCIDENT_REPORTS__PECS"))

          val pecsReport = reportRepository.saveAndFlush(
            buildReport(
              reportReference = "11124141",
              location = "NOU",
              reportTime = now,
              generateStaffInvolvement = 3,
              generatePrisonerInvolvement = 2,
            ),
          )

          webTestClient.get().uri(url)
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.length()").isEqualTo(1)
            .json(
              """
                [
                  {
                    "id": "${pecsReport.id}",
                    "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${pecsReport.id}' target=\"_blank\">${pecsReport.reportReference}</a>",
                    "type": "${pecsReport.type.name}",
                    "type_description": "${pecsReport.type.description}",
                    "status": "${pecsReport.status.name}",
                    "status_description": "${pecsReport.status.description}",
                    "incident_date_and_time": "05/12/2023 11:34",
                    "reported_at": "05/12/2023",
                    "reported_by": "USER1",
                    "title": "${pecsReport.title}",
                    "description": "${pecsReport.description}",
                    "location": "${pecsReport.location}",
                    "pecs_region": "National Operations Unit",
                    "modified_at": "05/12/2023 12:34"
                  }
                ]
              """.trimIndent(),
            )
        }
      }
    }

    @DisplayName("GET /reports/incident-with-people")
    @Nested
    inner class RunReportIncidentWithPeopleByPeople {
      private val url = "/reports/incident-with-people"

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(url + "/by-prisoner"),
          systemRole,
        )
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report for staff`() {
          webTestClient.get().uri(url + "/by-staff")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
[
  {
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "05/12/2023 11:34",
    "reported_by": "USER1",
    "first_name": "First 1",
    "last_name": "Last 1, First 1",
    "staff_username": "staff-1",
    "comment": "Comment #1"
  },
  {
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "05/12/2023 11:34",
    "reported_by": "USER1",
    "first_name": "First 2",
    "last_name": "Last 2, First 2",
    "staff_username": "staff-2",
    "comment": "Comment #2"
  },
  {
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "05/12/2023 11:34",
    "reported_by": "USER1",
    "first_name": "First 3",
    "last_name": "Last 3, First 3",
    "staff_username": "staff-3",
    "comment": "Comment #3"
  }
]
              """.trimIndent(),
            )
        }

        @Test
        fun `returns a page of the report for prisoners`() {
          webTestClient.get().uri(url + "/by-prisoner")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
[
  {
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "05/12/2023 11:34",
    "reported_by": "USER1",
    "first_name": "First 1",
    "last_name": "Last 1, First 1",
    "comment": "Comment #1"
  },
  {
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "05/12/2023 11:34",
    "reported_by": "USER1",
    "first_name": "First 2",
    "last_name": "Last 2, First 2",
    "comment": "Comment #2"
  }
]
              """.trimIndent(),
            )
        }
      }
    }

    @DisplayName("GET /reports/incident-count")
    @Nested
    inner class RunReportCountByPeriod {
      private val url = "/reports/incident-count/"

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(url + "by-week"),
          systemRole,
        )
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report for a count by day`() {
          webTestClient.get().uri(url + "by-location-per-day")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
              [
                {
                  "start_date": "05/12/2023",
                  "location": "MDI",
                  "type": "FINDS",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?fromDate=05/12/2023&toDate=05/12/2023&location=MDI&incidentType=FINDS' target=\"_blank\">1</a>"
                }
              ]
              """.trimIndent(),
            )
        }

        @Test
        fun `returns a page of the report for a count by location per week`() {
          webTestClient.get().uri(url + "by-location-per-week")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
            [
              {
                "start_date": "04/12/2023",
                "location": "MDI",
                "type": "FINDS",
                "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?fromDate=05/12/2023&toDate=05/12/2023&location=MDI&incidentType=FINDS' target=\"_blank\">1</a>"
              }
            ]
              """.trimIndent(),
            )
        }

        @Test
        fun `returns a page of the report for a count by location per month`() {
          webTestClient.get().uri(url + "by-location-per-month")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
              [
                {
                  "start_date": "Dec-2023",
                  "location": "MDI",
                  "type": "FINDS",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?fromDate=05/12/2023&toDate=05/12/2023&location=MDI&incidentType=FINDS' target=\"_blank\">1</a>"
                }
              ]
              """.trimIndent(),
            )
        }
      }
    }

    @DisplayName("GET /reports/prisoner-count")
    @Nested
    inner class RunReportPrisonerCount {
      private val url = "/reports/prisoner-count/"

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun endpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri(url + "per-type"),
          systemRole,
        )
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report for a count of prisoners`() {
          webTestClient.get().uri(url + "/per-type")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              """
[
  {
    "month_year": "Dec-2023",
    "min_date": "05/12/2023",
    "max_date": "05/12/2023",
    "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0002AA' target=\"_blank\">A0002AA</a>",
    "first_name": "First 2",
    "last_name": "Last 2, First 2",
    "type": "FINDS",
    "type_description": "Find of illicit items",
    "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?searchID=A0002AA&fromDate=05/12/2023&toDate=05/12/2023&location=MDI&incidentType=FINDS' target=\"_blank\">1</a>"
  },
  {
    "month_year": "Dec-2023",
    "min_date": "05/12/2023",
    "max_date": "05/12/2023",
    "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0001AA' target=\"_blank\">A0001AA</a>",
    "first_name": "First 1",
    "last_name": "Last 1, First 1",
    "type": "FINDS",
    "type_description": "Find of illicit items",
    "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?searchID=A0001AA&fromDate=05/12/2023&toDate=05/12/2023&location=MDI&incidentType=FINDS' target=\"_blank\">1</a>"
  }
]
              """.trimIndent(),
            )
        }
      }
    }
  }
}
