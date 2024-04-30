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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentNumber
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ReportRepositoryTest : IntegrationTestBase() {

  @Autowired
  lateinit var reportRepository: ReportRepository

  @Autowired
  lateinit var eventRepository: EventRepository

  private val now = LocalDateTime.now(clock)
  private val hourAgo = now.minusHours(1)
  private val halfHourAgo = now.minusMinutes(30)
  private val quarterHourAgo = now.minusMinutes(15)

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    eventRepository.deleteAll()
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
    report.addStatusHistory(Status.DRAFT, now, "user5")
    report.addEvidence("evidence2", "description2")
    report.addStaffInvolved(StaffRole.FIRST_ON_SCENE, "user1")
    report.addPrisonerInvolved("A1234AA", PrisonerRole.VICTIM)
    report.addLocation("MDI-1-1-1", "CELL", "Other stuff")

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneByIncidentNumber(report.incidentNumber) ?: throw EntityNotFoundException()

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

    report = reportRepository.findOneByIncidentNumber(report.incidentNumber) ?: throw EntityNotFoundException()
    report.changeType(Type.ASSAULT, now, "user5")

    report.addQuestion("SOME_QUESTION", "Another question?")
      .addResponse("YES", "Yes", "user1", now)
      .addResponse("NO", "No", "user1", now)
      .addResponse("MAYBE", "Maybe", "user1", now)
      .addResponse("OTHER", "Other", "user1", now)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneByIncidentNumber(report.incidentNumber) ?: throw EntityNotFoundException()
    assertThat(report.status).isEqualTo(Status.AWAITING_ANALYSIS)
    assertThat(report.getType()).isEqualTo(Type.ASSAULT)
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
