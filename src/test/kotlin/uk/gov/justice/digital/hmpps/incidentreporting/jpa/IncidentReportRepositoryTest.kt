package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentEventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentReportNumber
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class IncidentReportRepositoryTest : IntegrationTestBase() {

  @Autowired
  lateinit var reportRepository: IncidentReportRepository

  @Autowired
  lateinit var eventRepository: IncidentEventRepository

  private val now = LocalDateTime.now(clock)
  private val whenIncidentHappened = now.minusHours(1)

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()
  }

  @Test
  fun `create an incident report`() {
    var incidentReport =
      reportRepository.save(
        IncidentReport(
          incidentNumber = reportRepository.generateIncidentReportNumber(),
          incidentDateAndTime = whenIncidentHappened,
          incidentType = IncidentType.SELF_HARM,
          summary = "A Summary",
          incidentDetails = "An incident occurred",
          reportedBy = "user1",
          prisonId = "MDI",
          event = IncidentEvent(
            eventId = eventRepository.generateEventId(),
            eventDateAndTime = whenIncidentHappened,
            prisonId = "MDI",
            eventDetails = "An event occurred",
            createdDate = now,
            lastModifiedDate = now,
            lastModifiedBy = "user1",
          ),
          reportedDate = now,
          assignedTo = "user2",
          createdDate = now,
          lastModifiedDate = now,
          lastModifiedBy = "user1",
        ),
      )

    incidentReport.addEvidence("evidence2", "description2")
    incidentReport.addStaffInvolved(StaffRole.FIRST_ON_SCENE, "user1")
    incidentReport.addPrisonerInvolved("A1234AA", PrisonerRole.VICTIM)
    incidentReport.addIncidentLocation("MDI-1-1-1", "CELL", "Other stuff")

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    incidentReport = reportRepository.findOneByIncidentNumber(incidentReport.incidentNumber) ?: throw EntityNotFoundException()

    val incidentData1 = incidentReport.addIncidentData("WHERE_OCCURRED", "Where did this occur?")
    incidentData1
      .addAnswer("DETOX_UNIT", "They hurt themselves", "user1", now)
      .addAnswer("CELL", "In the cell", "user1", now)

    incidentData1.attachLocation(incidentReport.locations[0])
    incidentData1.attachPrisonerInvolvement(incidentReport.prisonersInvolved[0])
    incidentData1.attachStaffInvolvement(incidentReport.staffInvolved[0])
    incidentData1.attachEvidence(incidentReport.evidence[0])

    incidentReport.addIncidentData("METHOD", "Method Used to hurt themselves?")
      .addAnswer("KNIFE", "They used a knife", "user1", now)
      .addAnswer("OTHER", "They used something else", "user1", now)

    incidentReport.addIncidentData("BLAH", "Blah?")
      .addAnswer("HEAD", "Head", "user1", now)
      .addAnswer("ARM", "Arm", "user1", now)

    val before1 = now.minusMinutes(5)
    incidentReport.addIncidentHistory(IncidentType.FINDS, before1, "user2")
      .addHistoricalResponse("dataItem3", "dataItemDescription3")
      .addAnswer("response1", "Some information", "user1", before1)
      .addAnswer("response2", "Some information", "user1", before1)
      .addAnswer("response3", "Some information", "user1", before1)

    val before2 = now.minusMinutes(2)
    incidentReport.addIncidentHistory(IncidentType.ASSAULT, before2, "user1")
      .addHistoricalResponse("dataItem1", "dataItemDescription1")
      .addAnswer("response1", "Some information", "user1", before2)
      .addAnswer("response2", "Some information", "user1", before2)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    incidentReport = reportRepository.findOneByIncidentNumber(incidentReport.incidentNumber) ?: throw EntityNotFoundException()
    incidentReport.changeIncidentType(IncidentType.ASSAULT, LocalDateTime.now(clock), "user5")

    incidentReport.addIncidentData("SOME_QUESTION", "Another question?")
      .addAnswer("YES", "Yes", "user1", now)
      .addAnswer("NO", "No", "user1", now)
      .addAnswer("MAYBE", "Maybe", "user1", now)
      .addAnswer("OTHER", "Other", "user1", now)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    incidentReport = reportRepository.findOneByIncidentNumber(incidentReport.incidentNumber) ?: throw EntityNotFoundException()
    assertThat(incidentReport).isNotNull

    assertThat(incidentReport.getIncidentType()).isEqualTo(IncidentType.ASSAULT)
    assertThat(incidentReport.getIncidentData()).hasSize(1)
    assertThat(incidentReport.history).hasSize(3)
    assertThat(incidentReport.history[2].historyOfResponses).hasSize(3)
  }
}
