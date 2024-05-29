package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.EntityNotFoundException
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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentNumber
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByPrisonId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterBySource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByStatuses
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByType
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ReportRepositoryTest : IntegrationTestBase() {

  @Autowired
  lateinit var reportRepository: ReportRepository

  @Autowired
  lateinit var eventRepository: EventRepository

  private val hourAgo = now.minusHours(1)
  private val halfHourAgo = now.minusMinutes(30)
  private val quarterHourAgo = now.minusMinutes(15)

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()
  }

  @DisplayName("filtering reports")
  @Nested
  inner class Filtering {
    private val firstPageSortedById = PageRequest.of(0, 20)
      .withSort(Sort.Direction.ASC, "id")

    @Test
    fun `can filter reports by simple property specification`() {
      val report = reportRepository.save(
        buildIncidentReport(
          incidentNumber = "12345",
          reportTime = now.minusDays(1),
        ),
      )

      val matchingSpecifications = listOf(
        filterByPrisonId("MDI"),
        filterBySource(InformationSource.DPS),
        filterByStatuses(Status.DRAFT),
        filterByType(Type.FINDS),
        filterByIncidentDateFrom(now.toLocalDate().minusDays(2)),
        filterByIncidentDateUntil(now.toLocalDate()),
        filterByReportedDateFrom(now.toLocalDate().minusDays(2)),
        filterByReportedDateUntil(now.toLocalDate()),
      )
      matchingSpecifications.forEach { specification ->
        val reportsFound = reportRepository.findAll(
          specification,
          firstPageSortedById,
        )
        assertThat(reportsFound.totalElements).isEqualTo(1)
        assertThat(reportsFound.content[0].id).isEqualTo(report.id)
      }

      val nonMatchingSpecifications = listOf(
        filterByPrisonId("LEI"),
        filterBySource(InformationSource.NOMIS),
        filterByStatuses(Status.AWAITING_ANALYSIS),
        filterByType(Type.FOOD_REFUSAL),
        filterByIncidentDateFrom(now.toLocalDate()),
        filterByIncidentDateUntil(now.toLocalDate().minusDays(2)),
        filterByReportedDateFrom(now.toLocalDate()),
        filterByReportedDateUntil(now.toLocalDate().minusDays(2)),
      )
      nonMatchingSpecifications.forEach { specification ->
        val reportsFound = reportRepository.findAll(
          specification,
          firstPageSortedById,
        )
        assertThat(reportsFound.totalElements).isZero()
        assertThat(reportsFound.content).isEmpty()
      }
    }

    @Test
    fun `can filter reports by a combination of specifications`() {
      val report1Id = reportRepository.save(
        buildIncidentReport(
          incidentNumber = "12345",
          reportTime = now.minusDays(3),
          prisonId = "MDI",
          source = InformationSource.DPS,
          status = Status.AWAITING_ANALYSIS,
          type = Type.ASSAULT,
        ),
      ).id!!
      val report2Id = reportRepository.save(
        buildIncidentReport(
          incidentNumber = "12346",
          reportTime = now.minusDays(2),
          prisonId = "LEI",
          source = InformationSource.DPS,
          status = Status.AWAITING_ANALYSIS,
          type = Type.FINDS,
        ),
      ).id!!
      val report3Id = reportRepository.save(
        buildIncidentReport(
          incidentNumber = "IR-0000000001124143",
          reportTime = now.minusDays(1),
          prisonId = "MDI",
          source = InformationSource.NOMIS,
          status = Status.DRAFT,
          type = Type.FINDS,
        ),
      ).id!!

      fun assertSpecificationReturnsReports(specification: Specification<Report>, reportIds: List<UUID>) {
        val reportsFound = reportRepository.findAll(
          specification,
          firstPageSortedById,
        ).map { it.id }
        assertThat(reportsFound.content).isEqualTo(reportIds)
      }

      assertSpecificationReturnsReports(
        filterByPrisonId("MDI"),
        listOf(report1Id, report3Id),
      )
      assertSpecificationReturnsReports(
        filterByPrisonId("MDI")
          .and(filterBySource(InformationSource.NOMIS)),
        listOf(report3Id),
      )
      assertSpecificationReturnsReports(
        filterByPrisonId("LEI")
          .and(filterBySource(InformationSource.NOMIS)),
        emptyList(),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterBySource(InformationSource.DPS)),
        listOf(report1Id, report2Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.DRAFT)
          .or(filterByStatuses(Status.AWAITING_ANALYSIS)),
        listOf(report1Id, report2Id, report3Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(listOf(Status.DRAFT, Status.AWAITING_ANALYSIS)),
        listOf(report1Id, report2Id, report3Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterBySource(InformationSource.DPS))
          .and(filterByType(Type.FINDS)),
        listOf(report2Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterByPrisonId("LEI"))
          .and(filterBySource(InformationSource.DPS))
          .and(filterByType(Type.FINDS)),
        listOf(report2Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterBySource(InformationSource.DPS))
          .and(filterByPrisonId("MDI"))
          .and(filterByType(Type.FINDS)),
        emptyList(),
      )
      assertSpecificationReturnsReports(
        filterByIncidentDateFrom(now.toLocalDate().minusDays(3))
          .and(filterByIncidentDateUntil(now.toLocalDate().minusDays(3)))
          .and(filterByType(Type.ASSAULT)),
        listOf(report1Id),
      )
      assertSpecificationReturnsReports(
        filterByReportedDateFrom(now.toLocalDate().minusDays(4))
          .and(filterByReportedDateUntil(now.toLocalDate().minusDays(4))),
        emptyList(),
      )
    }
  }

  @Test
  fun `create an incident report`() {
    var report =
      reportRepository.save(
        Report(
          incidentNumber = reportRepository.generateIncidentNumber(),
          incidentDateAndTime = hourAgo,
          status = Status.AWAITING_ANALYSIS,
          type = Type.SELF_HARM,
          title = "A summary",
          description = "An incident occurred",
          reportedBy = "user1",
          prisonId = "MDI",
          event = Event(
            eventId = eventRepository.generateEventId(),
            eventDateAndTime = hourAgo,
            prisonId = "MDI",
            title = "Event summary",
            description = "An event occurred",
            createdAt = now,
            modifiedAt = now,
            modifiedBy = "user1",
          ),
          reportedAt = now,
          assignedTo = "user2",
          createdAt = now,
          modifiedAt = now,
          modifiedBy = "user1",
        ),
      )
    report.addStatusHistory(Status.DRAFT, now, "user5")
    report.addEvidence("evidence2", "description2")
    report.addStaffInvolved(StaffRole.FIRST_ON_SCENE, "user1")
    report.addPrisonerInvolved("A1234AA", PrisonerRole.VICTIM)
    report.addLocation("MDI-1-1-1", "CELL", "Other stuff")

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneEagerlyByIncidentNumber(report.incidentNumber)
      ?: throw EntityNotFoundException()

    report.addQuestion("WHERE_OCCURRED", "Where did this occur?")
      .addResponse("DETOX_UNIT", "They hurt themselves", "user1", now)
      .addResponse("CELL", "In the cell", "user1", now)

    report.addQuestion("METHOD", "Method Used to hurt themselves?")
      .addResponse("KNIFE", "They used a knife", "user1", now)
      .addResponse("OTHER", "They used something else", "user1", now)

    report.addQuestion("BLAH", "Blah?")
      .addResponse("HEAD", "Head", "user1", now)
      .addResponse("ARM", "Arm", "user1", now)

    report.addHistory(Type.FINDS, halfHourAgo, "user2")
      .addQuestion("FINDS-Q1", "Finds question 1")
      .addResponse("response1", "Some information 1", "user1", halfHourAgo)
      .addResponse("response2", "Some information 2", "user1", halfHourAgo)
      .addResponse("response3", "Some information 3", "user1", halfHourAgo)

    report.addHistory(Type.ASSAULT, quarterHourAgo, "user1")
      .addQuestion("ASSAULT-Q1", "Assault question 1")
      .addResponse("response4", "Some information 4", "user1", quarterHourAgo)
      .addResponse("response5", "Some information 5", "user1", quarterHourAgo)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneEagerlyByIncidentNumber(report.incidentNumber)
      ?: throw EntityNotFoundException()
    report.changeType(Type.ASSAULT, now, "user5")

    report.addQuestion("SOME_QUESTION", "Another question?")
      .addResponse("YES", "Yes", "user1", now)
      .addResponse("NO", "No", "user1", now)
      .addResponse("MAYBE", "Maybe", "user1", now)
      .addResponse("OTHER", "Other", "user1", now)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneEagerlyByIncidentNumber(report.incidentNumber)
      ?: throw EntityNotFoundException()
    assertThat(report.status).isEqualTo(Status.AWAITING_ANALYSIS)
    assertThat(report.type).isEqualTo(Type.ASSAULT)
    assertThat(report.getQuestions()).hasSize(1)
    assertThat(report.getQuestions()[0].code).isEqualTo("SOME_QUESTION")
    assertThat(report.getQuestions()[0].getResponses()).hasSize(4)
    assertThat(report.history).hasSize(3)
    assertThat(report.history[2].questions).hasSize(3)
    assertThat(report.history[2].questions[1].getResponses()).hasSize(2)
    assertThat(report.history[2].questions[1].getResponses()[1].response).isEqualTo("OTHER")
    assertThat(report.historyOfStatuses).hasSize(1)
    assertThat(report.historyOfStatuses[0].status).isEqualTo(Status.DRAFT)
  }
}
