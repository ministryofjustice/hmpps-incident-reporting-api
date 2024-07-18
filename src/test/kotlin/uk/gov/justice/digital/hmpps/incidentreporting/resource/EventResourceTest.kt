package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildEvent
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import java.time.Clock

@DisplayName("Event resource")
class EventResourceTest : SqsIntegrationTestBase() {
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

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()
  }

  @DisplayName("GET /incident-events")
  @Nested
  inner class GetEvents {
    private val url = "/incident-events"

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
      fun `cannot get too large a page of events`() {
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
            assertThat(it).contains("No property 'missing' found for type 'Event'")
          }
      }

      @ParameterizedTest(name = "cannot filter by invalid `{0}`")
      @ValueSource(
        strings = [
          "prisonId=",
          "prisonId=M",
          "prisonId=Moorland+(HMP)",
          "eventDateFrom=2024",
          "eventDateFrom=yesterday",
          "eventDateUntil=1%2F1%2F2020",
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
      // tests for single event responses check JSON much more thoroughly

      @Test
      fun `returns empty list when there are no events`() {
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
              "sort": ["eventDateAndTime,DESC"]
            }""",
            true,
          )
      }

      @Test
      fun `can get first page of events`() {
        val existingEvent = eventRepository.save(
          buildEvent(
            eventReference = "IE-0000000001124143",
            eventDateAndTime = now.minusHours(1),
            reportDateAndTime = now,
          ),
        )
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """{
              "content": [{
                "id": ${existingEvent.id},
                "eventReference": "IE-0000000001124143",
                "eventDateAndTime": "2023-12-05T11:34:56"
              }],
              "number": 0,
              "size": 20,
              "numberOfElements": 1,
              "totalElements": 1,
              "totalPages": 1,
              "sort": ["eventDateAndTime,DESC"]
            }""",
            false,
          )
      }

      @DisplayName("when many events exist")
      @Nested
      inner class WhenManyEventsExist {
        @BeforeEach
        fun setUp() {
          eventRepository.saveAll(
            listOf("IE-0000000001124143", "IE-0000000001017203", "IE-0000000001006603", "94728", "31934")
              .mapIndexed { index, eventReference ->
                val daysBefore = index.toLong()
                val reportDateAndTime = now.minusDays(daysBefore)
                buildEvent(
                  eventReference = eventReference,
                  eventDateAndTime = reportDateAndTime.minusHours(1),
                  reportDateAndTime = reportDateAndTime,
                  prisonId = if (index < 2) "LEI" else "MDI",
                )
              },
          )
        }

        @Test
        fun `can get first page of events`() {
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
                  "eventReference": "IE-0000000001124143",
                  "eventDateAndTime": "2023-12-05T11:34:56"
                },
                {
                  "eventReference": "IE-0000000001017203",
                  "eventDateAndTime": "2023-12-04T11:34:56"
                },
                {
                  "eventReference": "IE-0000000001006603",
                  "eventDateAndTime": "2023-12-03T11:34:56"
                },
                {
                  "eventReference": "94728",
                  "eventDateAndTime": "2023-12-02T11:34:56"
                },
                {
                  "eventReference": "31934",
                  "eventDateAndTime": "2023-12-01T11:34:56"
                }
              ],
              "number": 0,
              "size": 20,
              "numberOfElements": 5,
              "totalElements": 5,
              "totalPages": 1,
              "sort": ["eventDateAndTime,DESC"]
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
                    "eventReference": "IE-0000000001124143",
                    "eventDateAndTime": "2023-12-05T11:34:56"
                  },
                  {
                    "eventReference": "IE-0000000001017203",
                    "eventDateAndTime": "2023-12-04T11:34:56"
                  }
                ],
                "number": 0,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["eventDateAndTime,DESC"]
              }""",
              false,
            )
        }

        @Test
        fun `can get another page of events`() {
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
                    "eventReference": "IE-0000000001006603",
                    "eventDateAndTime": "2023-12-03T11:34:56"
                  },
                  {
                    "eventReference": "94728",
                    "eventDateAndTime": "2023-12-02T11:34:56"
                  }
                ],
                "number": 1,
                "size": 2,
                "numberOfElements": 2,
                "totalElements": 5,
                "totalPages": 3,
                "sort": ["eventDateAndTime,DESC"]
              }""",
              false,
            )
        }

        @ParameterizedTest(name = "can sort events by {0}")
        @ValueSource(
          strings = [
            "eventDateAndTime,ASC",
            "eventDateAndTime,DESC",
            "eventReference,ASC",
            "eventReference,DESC",
            "id,ASC",
            "id,DESC",
          ],
        )
        fun `can sort events`(sortParam: String) {
          val expectedEventReferences = mapOf(
            "eventDateAndTime,ASC" to listOf("31934", "94728", "IE-0000000001006603", "IE-0000000001017203", "IE-0000000001124143"),
            "eventDateAndTime,DESC" to listOf("IE-0000000001124143", "IE-0000000001017203", "IE-0000000001006603", "94728", "31934"),
            "eventReference,ASC" to listOf("31934", "94728", "IE-0000000001006603", "IE-0000000001017203", "IE-0000000001124143"),
            "eventReference,DESC" to listOf("IE-0000000001124143", "IE-0000000001017203", "IE-0000000001006603", "94728", "31934"),
            // id, being a UUIDv7, should follow table insertion order (i.e. what setUp methods do above)
            "id,ASC" to listOf("IE-0000000001124143", "IE-0000000001017203", "IE-0000000001006603", "94728", "31934"),
            "id,DESC" to listOf("31934", "94728", "IE-0000000001006603", "IE-0000000001017203", "IE-0000000001124143"),
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
            ).jsonPath("content[*].eventReference").value<List<String>> {
              assertThat(it).isEqualTo(expectedEventReferences)
            }
        }

        @ParameterizedTest(name = "can filter events by `{0}`")
        @CsvSource(
          value = [
            "''                                                 | 5",
            "prisonId=MDI                                       | 3",
            "prisonId=LEI                                       | 2",
            "eventDateFrom=2023-12-05                           | 1",
            "eventDateFrom=2023-12-04                           | 2",
            "eventDateUntil=2023-12-03                          | 3",
            "eventDateUntil=2023-12-03                          | 3",
            "eventDateFrom=2023-12-02&eventDateUntil=2023-12-02 | 1",
          ],
          delimiter = '|',
        )
        fun `can filter events`(params: String, count: Int) {
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

  @DisplayName("GET /incident-events/{id}")
  @Nested
  inner class GetEventById {
    private lateinit var existingEvent: Event
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      existingEvent = eventRepository.save(
        buildEvent(
          eventReference = "IE-0000000001124143",
          eventDateAndTime = now.minusHours(1),
          reportDateAndTime = now,
        ),
      )
      url = "/incident-events/${existingEvent.id}"
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
      fun `cannot get a event by ID if it is not found`() {
        webTestClient.get().uri("/incident-events/11111111-2222-3333-4444-555555555555")
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
      fun `can get a event by ID`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": ${existingEvent.id},
              "eventReference": "IE-0000000001124143",
              "eventDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An event occurred",
              "description": "Details of the event",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1"
            }
            """,
            true,
          )
      }
    }
  }

  @DisplayName("GET /incident-events/reference/{reference}")
  @Nested
  inner class GetEventByReference {
    private lateinit var existingEvent: Event
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      existingEvent = eventRepository.save(
        buildEvent(
          eventReference = "IE-0000000001124143",
          eventDateAndTime = now.minusHours(1),
          reportDateAndTime = now,
        ),
      )
      url = "/incident-events/reference/${existingEvent.eventReference}"
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
      fun `cannot get a event by reference if it is not found`() {
        webTestClient.get().uri("/incident-events/reference/IE-11111111")
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
      fun `can get a event by reference`() {
        webTestClient.get().uri(url)
          .headers(setAuthorisation(roles = listOf("ROLE_VIEW_INCIDENT_REPORTS"), scopes = listOf("read")))
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """ 
            {
              "id": ${existingEvent.id},
              "eventReference": "IE-0000000001124143",
              "eventDateAndTime": "2023-12-05T11:34:56",
              "prisonId": "MDI",
              "title": "An event occurred",
              "description": "Details of the event",
              "createdAt": "2023-12-05T12:34:56",
              "modifiedAt": "2023-12-05T12:34:56",
              "modifiedBy": "USER1"
            }
            """,
            true,
          )
      }
    }
  }
}
