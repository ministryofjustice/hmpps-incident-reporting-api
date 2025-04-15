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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.helper.elementAtWrapped
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

  lateinit var existingReport1: Report
  lateinit var existingReport2: Report

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    existingReport1 = reportRepository.save(
      buildReport(
        reportReference = "11124143",
        reportTime = now,
        generateStaffInvolvement = 3,
        generatePrisonerInvolvement = 2,
      ),
    )

    existingReport2 = reportRepository.save(
      buildReport(
        reportReference = "11124199",
        reportTime = now,
        generateStaffInvolvement = 1,
        generatePrisonerInvolvement = 1,
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
          .expectBody().jsonPath("$.length()").isEqualTo(6)
          .jsonPath("$[0].authorised").isEqualTo("true")
      }

      @Test
      fun `returns the definitions of all the reports but not authorises as no user in context`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(user = null, roles = listOf(systemRole), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().jsonPath("$.length()").isEqualTo(6)
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
          .jsonPath("$.length()").isEqualTo(6)
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
            // language=json
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
            """,
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
              // language=json
              """
              [
                {
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_at": "05/12/2023",
                  "reported_by": "USER1",
                  "title": "Incident Report 11124143",
                  "description": "A new incident created in the new service of type find of illicit items",
                  "location": "MDI",
                  "modified_at": "05/12/2023 12:34"
                },
                {
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_at": "05/12/2023",
                  "reported_by": "USER1",
                  "title": "Incident Report 11124199",
                  "description": "A new incident created in the new service of type find of illicit items",
                  "location": "MDI",
                  "modified_at": "05/12/2023 12:34"
                }
              ]
              """,
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
              // language=json
              """
              [
                {
                  "id": "${pecsReport.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${pecsReport.id}' target=\"_blank\">${pecsReport.reportReference}</a>",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
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
              """,
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
        @DisplayName("by prisoner role and scope")
        @TestFactory
        fun prisonerEndpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri("$url/by-prisoner"),
          systemRole,
        )

        @DisplayName("by staff role and scope")
        @TestFactory
        fun staffEndpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri("$url/by-staff"),
          systemRole,
        )
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report for staff`() {
          webTestClient.get().uri("$url/by-staff")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              // language=json
              """
              [
                {
                  "id": "${existingReport1.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport1.id}' target=\"_blank\">11124143</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 1",
                  "last_name": "Last 1, First 1",
                  "staff_username": "staff-1",
                  "comment": "Comment #1"
                },
                {
                  "id": "${existingReport1.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport1.id}' target=\"_blank\">11124143</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 2",
                  "last_name": "Last 2, First 2",
                  "staff_username": "staff-2",
                  "comment": "Comment #2"
                },
                {
                  "id": "${existingReport1.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport1.id}' target=\"_blank\">11124143</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 3",
                  "last_name": "Last 3, First 3",
                  "staff_username": "staff-3",
                  "comment": "Comment #3"
                },
                {
                  "id": "${existingReport2.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport2.id}' target=\"_blank\">11124199</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 1",
                  "last_name": "Last 1, First 1",
                  "staff_username": "staff-1",
                  "comment": "Comment #1"
                }
              ]
              """,
            )
        }

        @Test
        fun `returns a page of the report for prisoners`() {
          webTestClient.get().uri("$url/by-prisoner")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              // language=json
              """
              [
                {
                  "id": "${existingReport1.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport1.id}' target=\"_blank\">11124143</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 1",
                  "last_name": "Last 1, First 1",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0001AA' target=\"_blank\">A0001AA</a>",
                  "comment": "Comment #1"
                },
                {
                  "id": "${existingReport1.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport1.id}' target=\"_blank\">11124143</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 2",
                  "last_name": "Last 2, First 2",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0002AA' target=\"_blank\">A0002AA</a>",
                  "comment": "Comment #2"
                },
                {
                  "id": "${existingReport2.id}",
                  "report_reference": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports/${existingReport2.id}' target=\"_blank\">11124199</a>",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "reported_by": "USER1",
                  "first_name": "First 1",
                  "last_name": "Last 1, First 1",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0001AA' target=\"_blank\">A0001AA</a>",
                  "comment": "Comment #1"
                }
              ]
              """,
            )
        }

        @Test
        fun `returns a page of the report for self harm`() {
          val selfHarm = buildReport(
            reportReference = "222000",
            reportTime = now,
            location = "MDI",
            type = Type.SELF_HARM_1,
            generatePrisonerInvolvement = 1,
          )
          selfHarm.prisonersInvolved.first().prisonerRole = PrisonerRole.PERPETRATOR

          selfHarm.addQuestion(
            code = "1",
            question = "WHERE DID THE INCIDENT TAKE PLACE",
            1,
          ).addResponse(
            response = "CELL",
            additionalInformation = "H1",
            sequence = 0,
            recordedBy = "staff-1",
            recordedAt = now,
          )
          selfHarm.addQuestion(
            code = "1",
            question = "DID SELF HARM METHOD INVOLVE CUTTING",
            2,
          ).addResponse(response = "YES", sequence = 0, recordedBy = "staff-1", recordedAt = now)
          selfHarm.addQuestion(
            code = "1",
            question = "TYPE OF IMPLEMENT USED",
            3,
          ).addResponse(response = "Knife", sequence = 0, recordedBy = "staff-1", recordedAt = now)
          selfHarm.addQuestion(
            code = "1",
            question = "WHAT OTHER METHOD OF SELF HARM WAS INVOLVED",
            4,
          ).addResponse(response = "Mirror", sequence = 0, recordedBy = "staff-1", recordedAt = now)

          reportRepository.saveAndFlush(selfHarm)

          webTestClient.put().uri("/refresh-views")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk

          webTestClient.get().uri("$url/self-harm")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              // language=json
              """
              [
                {
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0001AA' target=\"_blank\">A0001AA</a>",
                  "last_name": "Last 1, First 1",
                  "first_name": "First 1",
                  "location": "MDI",
                  "incident_date_and_time": "05/12/2023 11:34",
                  "title": "Incident Report 222000",
                  "description": "A new incident created in the new service of type self harm",
                  "category": "Cutting",
                  "materials_used": "Knife",
                  "other_method": "Mirror",
                  "q1_location": "Cell",
                  "additional_comment": "H1"
                }
              ]
              """,
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
              // language=json
              """
              [
                {
                  "start_date": "05/12/2023",
                  "filter_date": "05/12/2023",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?fromDate=05/12/2023&toDate=05/12/2023&location=MDI&typeFamily=FIND' target=\"_blank\">2</a>"
                }
              ]
              """,
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
              // language=json
              """
              [
                {
                  "start_date": "04/12/2023",
                  "min_date": "05/12/2023",
                  "max_date": "05/12/2023",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?fromDate=05/12/2023&toDate=05/12/2023&location=MDI&typeFamily=FIND' target=\"_blank\">2</a>"
                }
              ]
              """,
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
              // language=json
              """
              [
                {
                  "start_date": "Dec-2023",
                  "min_date": "05/12/2023",
                  "max_date": "05/12/2023",
                  "location": "MDI",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?fromDate=05/12/2023&toDate=05/12/2023&location=MDI&typeFamily=FIND' target=\"_blank\">2</a>"
                }
              ]
              """,
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
          // Add a duplicate prisoner (A0002AA) to the report
          val prisonerIndex = 3
          existingReport1.addPrisonerInvolved(
            sequence = prisonerIndex - 1,
            prisonerNumber = "A%04dAA".format(2),
            firstName = "First 2",
            lastName = "Last 2",
            prisonerRole = PrisonerRole.entries.elementAtWrapped(prisonerIndex),
            outcome = PrisonerOutcome.entries.elementAtWrapped(prisonerIndex),
            comment = "Comment #$prisonerIndex",
          )
          reportRepository.save(existingReport1)

          webTestClient.get().uri("$url/per-type")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              // language=json
              """
              [
                {
                  "month_year": "Dec-2023",
                  "min_date": "05/12/2023",
                  "max_date": "05/12/2023",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0002AA' target=\"_blank\">A0002AA</a>",
                  "first_name": "First 2",
                  "last_name": "Last 2, First 2",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "location": "MDI",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?searchID=A0002AA&fromDate=05/12/2023&toDate=05/12/2023&location=MDI&typeFamily=FIND' target=\"_blank\">1</a>"
                },
                {
                  "month_year": "Dec-2023",
                  "min_date": "05/12/2023",
                  "max_date": "05/12/2023",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0001AA' target=\"_blank\">A0001AA</a>",
                  "first_name": "First 1",
                  "last_name": "Last 1, First 1",
                  "type": "FIND",
                  "type_description": "Find of illicit items",
                  "location": "MDI",
                  "num_of_incidents": "<a href='https://incident-reporting.hmpps.service.justice.gov.uk/reports?searchID=A0001AA&fromDate=05/12/2023&toDate=05/12/2023&location=MDI&typeFamily=FIND' target=\"_blank\">2</a>"
                }
              ]
              """,
            )
        }
      }
    }

    @DisplayName("GET /reports/incident-type-detail")
    @Nested
    inner class RunReportIncidentTypeDetail {
      private val url = "/reports/incident-type-detail"

      @DisplayName("is secured")
      @Nested
      inner class Security {
        @DisplayName("by role and scope")
        @TestFactory
        fun prisonerEndpointRequiresAuthorisation() = endpointRequiresAuthorisation(
          webTestClient.get().uri("$url/serious-sexual-assault"),
          systemRole,
        )
      }

      @DisplayName("works")
      @Nested
      inner class HappyPath {

        @Test
        fun `returns a page of the report for serious assault`() {
          val violence = buildReport(
            reportReference = "222001",
            reportTime = now,
            location = "MDI",
            type = Type.ASSAULT_5,
            generatePrisonerInvolvement = 2,
          )
          violence.prisonersInvolved.first().prisonerRole = PrisonerRole.FIGHTER
          violence.prisonersInvolved.last().prisonerRole = PrisonerRole.VICTIM

          violence.addQuestion(
            code = "1",
            question = "WAS A SERIOUS INJURY SUSTAINED",
            1,
          ).addResponse(response = "YES", sequence = 0, recordedBy = "staff-1", recordedAt = now)

          reportRepository.saveAndFlush(violence)

          webTestClient.put().uri("/refresh-views")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk

          webTestClient.get().uri("$url/serious-sexual-assault")
            .headers(setAuthorisation(roles = listOf(systemRole), scopes = listOf("read")))
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json(
              // language=json
              """
              [
                {
                  "incident_date_and_time": "05/12/2023 11:34",
                  "title": "Incident Report 222001",
                  "description": "A new incident created in the new service of type assault",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "location": "MDI",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0001AA' target=\"_blank\">A0001AA</a>",
                  "prisoner_role": "FIGHTER",
                  "prisoner_role_description": "Fighter",
                  "last_name": "Last 1, First 1",
                  "first_name": "First 1"
                },
                {
                  "incident_date_and_time": "05/12/2023 11:34",
                  "title": "Incident Report 222001",
                  "description": "A new incident created in the new service of type assault",
                  "status": "DRAFT",
                  "status_description": "Draft",
                  "location": "MDI",
                  "prisoner_number": "<a href='https://prisoner.digital.prison.service.justice.gov.uk/prisoner/A0002AA' target=\"_blank\">A0002AA</a>",
                  "prisoner_role": "VICTIM",
                  "prisoner_role_description": "Victim",
                  "last_name": "Last 2, First 2",
                  "first_name": "First 2"
                }
              ]
              """,
            )
        }
      }
    }
  }
}
