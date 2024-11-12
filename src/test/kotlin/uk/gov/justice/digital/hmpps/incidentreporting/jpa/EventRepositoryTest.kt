package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildEvent
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByEventDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByEventDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterEventsByPrisonIds
import java.util.UUID

@DisplayName("Event repository")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class EventRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var reportRepository: ReportRepository

  @Autowired
  lateinit var eventRepository: EventRepository

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
  }

  @DisplayName("filtering events")
  @Nested
  inner class Filtering {
    private val firstPageSortedById = PageRequest.of(0, 20)
      .withSort(Sort.Direction.ASC, "id")

    @Test
    fun `can filter events by simple property specification`() {
      val whenEventHappened = now.minusDays(1)
      val whenEventReported = whenEventHappened.plusHours(2)
      val event = eventRepository.save(
        buildEvent(
          eventReference = "11124143",
          eventDateAndTime = whenEventHappened,
          reportDateAndTime = whenEventReported,
        ),
      )

      val matchingSpecifications = listOf(
        filterEventsByPrisonIds("MDI"),
        filterEventsByEventDateFrom(now.toLocalDate().minusDays(2)),
        filterEventsByEventDateFrom(now.toLocalDate().minusDays(1)),
        filterEventsByEventDateUntil(now.toLocalDate().minusDays(1)),
        filterEventsByEventDateUntil(now.toLocalDate()),
      )
      matchingSpecifications.forEach { specification ->
        val eventsFound = eventRepository.findAll(
          specification,
          firstPageSortedById,
        )
        assertThat(eventsFound.totalElements).isEqualTo(1)
        assertThat(eventsFound.content[0].id).isEqualTo(event.id)
      }

      val nonMatchingSpecifications = listOf(
        filterEventsByPrisonIds("LEI"),
        filterEventsByEventDateFrom(now.toLocalDate()),
        filterEventsByEventDateUntil(now.toLocalDate().minusDays(2)),
      )
      nonMatchingSpecifications.forEach { specification ->
        val eventsFound = eventRepository.findAll(
          specification,
          firstPageSortedById,
        )
        assertThat(eventsFound.totalElements).isZero()
        assertThat(eventsFound.content).isEmpty()
      }
    }

    @Test
    fun `can filter events by a combination of specifications`() {
      val eventIds = eventRepository.saveAll(
        listOf(
          buildEvent(
            eventReference = "12345",
            eventDateAndTime = now.minusDays(3).minusHours(2),
            reportDateAndTime = now.minusDays(3),
            prisonId = "MDI",
          ),
          buildEvent(
            eventReference = "12346",
            eventDateAndTime = now.minusDays(2).minusHours(2),
            reportDateAndTime = now.minusDays(2),
            prisonId = "LEI",
          ),
          buildEvent(
            eventReference = "11124143",
            eventDateAndTime = now.minusDays(1).minusHours(2),
            reportDateAndTime = now.minusDays(1),
            prisonId = "MDI",
          ),
        ),
      ).map { it.id!! }
      val (event1Id, event2Id, event3Id) = eventIds

      fun assertSpecificationReturnsEvents(specification: Specification<Event>, eventIds: List<UUID>) {
        val eventsFound = eventRepository.findAll(
          specification,
          firstPageSortedById,
        ).map { it.id }
        assertThat(eventsFound.content).isEqualTo(eventIds)
      }

      assertSpecificationReturnsEvents(
        filterEventsByPrisonIds("MDI"),
        listOf(event1Id, event3Id),
      )
      assertSpecificationReturnsEvents(
        filterEventsByPrisonIds("MDI")
          .or(filterEventsByPrisonIds("LEI")),
        listOf(event1Id, event2Id, event3Id),
      )
      assertSpecificationReturnsEvents(
        filterEventsByPrisonIds("MDI", "LEI"),
        listOf(event1Id, event2Id, event3Id),
      )
      assertSpecificationReturnsEvents(
        filterEventsByPrisonIds("MDI")
          .and(filterEventsByEventDateUntil(now.toLocalDate().minusDays(2))),
        listOf(event1Id),
      )
    }
  }
}
