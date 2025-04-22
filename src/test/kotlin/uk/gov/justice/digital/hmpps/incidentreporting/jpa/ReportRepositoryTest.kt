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
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventReference
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateReportReference
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByLocations
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReference
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterBySource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByStatuses
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByTypes
import java.util.UUID

@DisplayName("Report repository")
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

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
  }

  @DisplayName("filtering reports")
  @Nested
  inner class Filtering {
    private val firstPageSortedById = PageRequest.of(0, 20)
      .withSort(Sort.Direction.ASC, "id")

    @Test
    fun `can filter reports by simple property specification`() {
      val report = reportRepository.save(
        buildReport(
          reportReference = "12345",
          reportTime = now.minusDays(1),
        ),
      )

      val matchingSpecifications = listOf(
        filterByLocations("MDI"),
        filterBySource(InformationSource.DPS),
        filterByStatuses(Status.DRAFT),
        filterByTypes(Type.FIND_6),
        filterByIncidentDateFrom(today.minusDays(2)),
        filterByIncidentDateUntil(today),
        filterByReportedDateFrom(today.minusDays(2)),
        filterByReportedDateUntil(today),
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
        filterByLocations("LEI"),
        filterBySource(InformationSource.NOMIS),
        filterByStatuses(Status.AWAITING_ANALYSIS),
        filterByTypes(Type.FOOD_REFUSAL_1),
        filterByIncidentDateFrom(today),
        filterByIncidentDateUntil(today.minusDays(2)),
        filterByReportedDateFrom(today),
        filterByReportedDateUntil(today.minusDays(2)),
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
      val reportIds = reportRepository.saveAll(
        listOf(
          buildReport(
            reportReference = "12345",
            reportTime = now.minusDays(3),
            location = "MDI",
            source = InformationSource.DPS,
            status = Status.AWAITING_ANALYSIS,
            type = Type.ASSAULT_5,
          ),
          buildReport(
            reportReference = "12346",
            reportTime = now.minusDays(2),
            location = "LEI",
            source = InformationSource.DPS,
            status = Status.AWAITING_ANALYSIS,
            type = Type.FIND_6,
          ),
          buildReport(
            reportReference = "11124143",
            reportTime = now.minusDays(1),
            location = "MDI",
            source = InformationSource.NOMIS,
            status = Status.DRAFT,
            type = Type.FIND_6,
          ),
        ),
      ).map { it.id!! }
      val (report1Id, report2Id, report3Id) = reportIds

      fun assertSpecificationReturnsReports(specification: Specification<Report>, reportIds: List<UUID>) {
        val reportsFound = reportRepository.findAll(
          specification,
          firstPageSortedById,
        ).map { it.id }
        assertThat(reportsFound.content).isEqualTo(reportIds)
      }

      assertSpecificationReturnsReports(
        filterByLocations("MDI"),
        listOf(report1Id, report3Id),
      )
      assertSpecificationReturnsReports(
        filterByLocations("MDI")
          .and(filterBySource(InformationSource.NOMIS)),
        listOf(report3Id),
      )
      assertSpecificationReturnsReports(
        filterByLocations("LEI")
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
        filterByLocations(listOf("LEI", "MDI")),
        listOf(report1Id, report2Id, report3Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterBySource(InformationSource.DPS))
          .and(filterByTypes(Type.FIND_6)),
        listOf(report2Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterByLocations("LEI"))
          .and(filterBySource(InformationSource.DPS))
          .and(filterByTypes(Type.FIND_6)),
        listOf(report2Id),
      )
      assertSpecificationReturnsReports(
        filterByStatuses(Status.AWAITING_ANALYSIS)
          .and(filterBySource(InformationSource.DPS))
          .and(filterByLocations("MDI"))
          .and(filterByTypes(Type.FIND_6)),
        emptyList(),
      )
      assertSpecificationReturnsReports(
        filterByIncidentDateFrom(today.minusDays(3))
          .and(filterByIncidentDateUntil(today.minusDays(3)))
          .and(filterByTypes(Type.ASSAULT_5)),
        listOf(report1Id),
      )
      assertSpecificationReturnsReports(
        filterByReportedDateFrom(today.minusDays(4))
          .and(filterByReportedDateUntil(today.minusDays(4))),
        emptyList(),
      )
      assertSpecificationReturnsReports(
        filterByReference("11124143")
          .and(filterByLocations("MDI")),
        listOf(report3Id),
      )
      assertSpecificationReturnsReports(
        filterByReference("11124143")
          .and(filterByLocations("LEI")),
        emptyList(),
      )
    }
  }

  @Test
  fun `create an incident report`() {
    var report =
      reportRepository.save(
        Report(
          reportReference = reportRepository.generateReportReference(),
          incidentDateAndTime = hourAgo,
          status = Status.AWAITING_ANALYSIS,
          type = Type.SELF_HARM_1,
          title = "A summary",
          description = "An incident occurred",
          reportedBy = "user1",
          location = "MDI",
          event = Event(
            eventReference = eventRepository.generateEventReference(),
            eventDateAndTime = hourAgo,
            location = "MDI",
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
    report.addStatusHistory(Status.AWAITING_ANALYSIS, now, "user1")
    report.addStaffInvolved(
      sequence = 0,
      staffUsername = "user1",
      firstName = "Mary",
      lastName = "Jones",
      staffRole = StaffRole.FIRST_ON_SCENE,
    )
    report.addPrisonerInvolved(
      sequence = 0,
      prisonerNumber = "A1234AA",
      firstName = "Trevor",
      lastName = "Smith",
      prisonerRole = PrisonerRole.VICTIM,
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneEagerlyByReportReference(report.reportReference)
      ?: throw EntityNotFoundException()

    report.addQuestion("WHERE_OCCURRED", "Where did this occur?", 1)
      .addResponse("DETOX_UNIT", null, 0, "They hurt themselves", "user1", now)
      .addResponse("CELL", null, 1, "In the cell", "user1", now)

    report.addQuestion("METHOD", "Method Used to hurt themselves?", 2)
      .addResponse("KNIFE", null, 0, "They used a knife", "user1", now)
      .addResponse("OTHER", null, 1, "They used something else", "user1", now)

    report.addQuestion("BLAH", "Blah?", 3)
      .addResponse("HEAD", null, 0, "Head", "user1", now)
      .addResponse("ARM", null, 1, "Arm", "user1", now)

    report.addHistory(Type.FIND_6, halfHourAgo, "user2")
      .addQuestion("FINDS-Q1", "Finds question 1", 1)
      .addResponse("response1", 0, null, "Some information 1", "user1", halfHourAgo)
      .addResponse("response2", 1, null, "Some information 2", "user1", halfHourAgo)
      .addResponse("response3", 2, null, "Some information 3", "user1", halfHourAgo)

    report.addHistory(Type.ASSAULT_5, quarterHourAgo, "user1")
      .addQuestion("ASSAULT-Q1", "Assault question 1", 1)
      .addResponse("response4", 0, null, "Some information 4", "user1", quarterHourAgo)
      .addResponse("response5", 1, null, "Some information 5", "user1", quarterHourAgo)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneEagerlyByReportReference(report.reportReference)
      ?: throw EntityNotFoundException()
    report.changeType(Type.ASSAULT_5, now, "user5")

    report.addQuestion("SOME_QUESTION", "Another question?", 4)
      .addResponse("YES", null, 0, "Yes", "user1", now)
      .addResponse("NO", null, 1, "No", "user1", now)
      .addResponse("MAYBE", null, 2, "Maybe", "user1", now)
      .addResponse("OTHER", null, 3, "Other", "user1", now)

    report.appendToDescription(
      "SOME_USER_1",
      "John",
      "Doe",
      now,
      "The prisoner was admitted to hospital",
    )
    report.appendToDescription(
      "SOME_USER_2",
      "Jane",
      "Doe",
      now,
      "The prisoner was discharged from hospital",
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneEagerlyByReportReference(report.reportReference)
      ?: throw EntityNotFoundException()
    assertThat(report.status).isEqualTo(Status.AWAITING_ANALYSIS)
    assertThat(report.type).isEqualTo(Type.ASSAULT_5)
    assertThat(report.description).isEqualTo("An incident occurred")
    assertThat(report.descriptionAddendums).hasSize(2)
    assertThat(report.descriptionAddendums.elementAt(0).createdBy).isEqualTo("SOME_USER_1")
    assertThat(report.descriptionAddendums.elementAt(0).text).isEqualTo("The prisoner was admitted to hospital")
    assertThat(report.descriptionAddendums.elementAt(1).createdBy).isEqualTo("SOME_USER_2")
    assertThat(report.descriptionAddendums.elementAt(1).text).isEqualTo("The prisoner was discharged from hospital")
    assertThat(report.questions).hasSize(1)
    assertThat(report.questions.elementAt(0).code).isEqualTo("SOME_QUESTION")
    assertThat(report.questions.elementAt(0).responses).hasSize(4)
    assertThat(report.history).hasSize(3)
    assertThat(report.history.elementAt(2).questions).hasSize(3)
    assertThat(report.history.elementAt(2).questions.elementAt(1).responses).hasSize(2)
    assertThat(report.history.elementAt(2).questions.elementAt(1).responses.elementAt(1).response).isEqualTo("OTHER")
    assertThat(report.historyOfStatuses).hasSize(2)
    assertThat(report.historyOfStatuses.elementAt(0).status).isEqualTo(Status.DRAFT)
    assertThat(report.historyOfStatuses.elementAt(1).status).isEqualTo(Status.AWAITING_ANALYSIS)
    assertThat(report.staffInvolved).hasSize(1)
    assertThat(report.staffInvolved.elementAt(0).lastName).isEqualTo("Jones")
    assertThat(report.prisonersInvolved).isEmpty()
  }
}
