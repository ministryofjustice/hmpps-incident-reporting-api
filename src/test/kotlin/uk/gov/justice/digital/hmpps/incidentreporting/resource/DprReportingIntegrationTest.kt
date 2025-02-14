package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
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
        "INCIDENT_REPORTS__RO",
      )
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns the definitions of all the reports`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath("$.length()").isEqualTo(3)
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
        "INCIDENT_REPORTS__RO",
      )
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns the definition of the report`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
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
    "specification": {
      "template": "list",
      "fields": [
        {
          "name": "id",
          "display": "Internal ID",
          "sortable": true,
          "defaultsort": false,
          "type": "string"
        },
        {
          "name": "report_reference",
          "display": "Report Reference"
        },
        {
          "name": "type",
          "display": "Incident type",
          "filter": {
            "type": "autocomplete",
            "mandatory": false,
            "pattern": null,
            "staticOptions": [
              {
                "name": "ABSCONDER",
                "display": "Absconder"
              },
              {
                "name": "ASSAULT",
                "display": "Assault"
              },
              {
                "name": "ATTEMPTED_ESCAPE_FROM_CUSTODY",
                "display": "Attempted escape from custody"
              },
              {
                "name": "ATTEMPTED_ESCAPE_FROM_ESCORT",
                "display": "Attempted escape from escort"
              },
              {
                "name": "BOMB_THREAT",
                "display": "Bomb threat"
              },
              {
                "name": "BREACH_OF_SECURITY",
                "display": "Breach of security"
              },
              {
                "name": "DAMAGE",
                "display": "Damage"
              },
              {
                "name": "DEATH_IN_CUSTODY",
                "display": "Death in custody"
              },
              {
                "name": "DEATH_OTHER",
                "display": "Death (other)"
              },
              {
                "name": "DISORDER",
                "display": "Disorder"
              },
              {
                "name": "DRONE_SIGHTING",
                "display": "Drone sighting"
              },
              {
                "name": "ESCAPE_FROM_CUSTODY",
                "display": "Escape from custody"
              },
              {
                "name": "ESCAPE_FROM_ESCORT",
                "display": "Escape from escort"
              },
              {
                "name": "FINDS",
                "display": "Finds"
              },
              {
                "name": "FIRE",
                "display": "Fire"
              },
              {
                "name": "FOOD_REFUSAL",
                "display": "Food refusal"
              },
              {
                "name": "FULL_CLOSE_DOWN_SEARCH",
                "display": "Full close down search"
              },
              {
                "name": "KEY_LOCK_INCIDENT",
                "display": "Key lock incident"
              },
              {
                "name": "MISCELLANEOUS",
                "display": "Miscellaneous"
              },
              {
                "name": "OLD_ASSAULT",
                "display": "Assault"
              }
            ]
          },
          "sortable": true,
          "defaultsort": false,
          "type": "string",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "status",
          "display": "Incident status",
          "wordWrap": null,
          "filter": {
            "type": "autocomplete",
            "mandatory": false,
            "pattern": null,
            "staticOptions": [
              {
                "name": "AWAITING_ANALYSIS",
                "display": "Awaiting analysis"
              },
              {
                "name": "CLOSED",
                "display": "Closed"
              },
              {
                "name": "DRAFT",
                "display": "Draft"
              },
              {
                "name": "DUPLICATE",
                "display": "Duplicate"
              },
              {
                "name": "IN_ANALYSIS",
                "display": "In analysis"
              },
              {
                "name": "INCIDENT_UPDATED",
                "display": "Incident updated"
              },
              {
                "name": "INFORMATION_AMENDED",
                "display": "Information amened"
              },
              {
                "name": "INFORMATION_REQUIRED",
                "display": "Information required"
              },
              {
                "name": "POST_INCIDENT_UPDATE",
                "display": "Post-incident update"
              }
            ]
          },
          "sortable": true,
          "defaultsort": false,
          "type": "string",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "incident_date_and_time",
          "display": "Incident occurrence time",
          "wordWrap": null,
          "filter": {
            "type": "daterange",
            "mandatory": false
          },
          "sortable": false,
          "defaultsort": false,
          "type": "date",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "reported_at",
          "display": "Reported on",
          "wordWrap": null,
          "filter": {
            "type": "daterange",
            "mandatory": false
          },
          "sortable": true,
          "defaultsort": false,
          "type": "date",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "reported_by",
          "display": "Reported by",
          "sortable": false,
          "defaultsort": false,
          "type": "string",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "title",
          "display": "Incident title",
          "sortable": false,
          "defaultsort": false,
          "type": "string",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "description",
          "display": "Incident description",
          "sortable": false,
          "defaultsort": false,
          "type": "string",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "location",
          "display": "Location of incident",
          "sortable": true,
          "defaultsort": false,
          "type": "string",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        },
        {
          "name": "modified_at",
          "display": "Last Updated",
          "wordWrap": null,
          "filter": {
            "type": "daterange",
            "mandatory": false,
            "interactive": false
          },
          "sortable": true,
          "defaultsort": true,
          "type": "date",
          "mandatory": false,
          "visible": true,
          "calculated": false,
          "header": false
        }
      ]
    },
    "printable": true
  }
}
            """.trimIndent(),
          )
      }
    }
  }

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
        "INCIDENT_REPORTS__RO",
      )
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns a page of the report`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "report_reference": "11124143",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "2023-12-05T11:34:56",
    "reported_at": "2023-12-05T12:34:56",
    "reported_by": "USER1",
    "title": "Incident Report 11124143",
    "description": "A new incident created in the new service of type Finds",
    "location": "MDI",
    "modified_at": "2023-12-05T12:34:56"
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
        "INCIDENT_REPORTS__RO",
      )
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns a page of the report for staff`() {
        webTestClient.get().uri(url + "/by-staff")
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "report_reference": "11124143",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "2023-12-05T11:34:56",
    "reported_by": "USER1",
    "first_name": "First 1",
    "last_name": "Last 1",
    "staff_username": "staff-1",
    "comment": "Comment #1"
  },
  {
    "report_reference": "11124143",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "2023-12-05T11:34:56",
    "reported_by": "USER1",
    "first_name": "First 2",
    "last_name": "Last 2",
    "staff_username": "staff-2",
    "comment": "Comment #2"
  },
  {
    "report_reference": "11124143",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "2023-12-05T11:34:56",
    "reported_by": "USER1",
    "first_name": "First 3",
    "last_name": "Last 3",
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
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "report_reference": "11124143",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "2023-12-05T11:34:56",
    "reported_by": "USER1",
    "first_name": "First 1",
    "last_name": "Last 1",
    "prisoner_number": "A0001AA",
    "comment": "Comment #1"
  },
  {
    "report_reference": "11124143",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "incident_date_and_time": "2023-12-05T11:34:56",
    "reported_by": "USER1",
    "first_name": "First 2",
    "last_name": "Last 2",
    "prisoner_number": "A0002AA",
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
        "INCIDENT_REPORTS__RO",
      )
    }

    @DisplayName("works")
    @Nested
    inner class HappyPath {

      @Test
      fun `returns a page of the report for a count by day`() {
        webTestClient.get().uri(url + "by-day")
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "start_date": "2023-12-05T00:00:00",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "num_of_incidents": 1
  }
]
            """.trimIndent(),
          )
      }

      @Test
      fun `returns a page of the report for a count by week`() {
        webTestClient.get().uri(url + "by-week")
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "start_date": "2023-12-04T00:00:00",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "num_of_incidents": 1
  }
]
            """.trimIndent(),
          )
      }

      @Test
      fun `returns a page of the report for a count by month`() {
        webTestClient.get().uri(url + "by-month")
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "start_date": "2023-12-01T00:00:00",
    "location": "MDI",
    "type": "FINDS",
    "status": "DRAFT",
    "num_of_incidents": 1
  }
]
            """.trimIndent(),
          )
      }

      @Test
      fun `returns a page of the report for a count by location per week`() {
        webTestClient.get().uri(url + "by-location-per-week")
          .headers(setAuthorisation(roles = listOf("ROLE_INCIDENT_REPORTS__RO"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            """
[
  {
    "start_date": "2023-12-04T00:00:00",
    "location": "MDI",
    "type": "FINDS",
    "num_of_incidents": 1
  }
]
            """.trimIndent(),
          )
      }
    }
  }
}
