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
  private val whenIncidentHappened = now.minusHours(1)

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
          incidentDateAndTime = whenIncidentHappened,
          type = Type.SELF_HARM,
          title = "A summary",
          description = "An incident occurred",
          reportedBy = "user1",
          prisonId = "MDI",
          event = Event(
            eventId = eventRepository.generateEventId(),
            eventDateAndTime = whenIncidentHappened,
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

    val before1 = now.minusMinutes(5)
    report.addHistory(Type.FINDS, before1, "user2")
      .addQuestion("dataItem3", "dataItemDescription3")
      .addResponse("response1", "Some information", "user1", before1)
      .addResponse("response2", "Some information", "user1", before1)
      .addResponse("response3", "Some information", "user1", before1)

    val before2 = now.minusMinutes(2)
    report.addHistory(Type.ASSAULT, before2, "user1")
      .addQuestion("dataItem1", "dataItemDescription1")
      .addResponse("response1", "Some information", "user1", before2)
      .addResponse("response2", "Some information", "user1", before2)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneByIncidentNumber(report.incidentNumber) ?: throw EntityNotFoundException()
    report.changeType(Type.ASSAULT, LocalDateTime.now(clock), "user5")

    report.addQuestion("SOME_QUESTION", "Another question?")
      .addResponse("YES", "Yes", "user1", now)
      .addResponse("NO", "No", "user1", now)
      .addResponse("MAYBE", "Maybe", "user1", now)
      .addResponse("OTHER", "Other", "user1", now)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    report = reportRepository.findOneByIncidentNumber(report.incidentNumber) ?: throw EntityNotFoundException()
    assertThat(report.getType()).isEqualTo(Type.ASSAULT)
    assertThat(report.getQuestions()).hasSize(1)
    assertThat(report.getQuestions()[0].code).isEqualTo("SOME_QUESTION")
    assertThat(report.getQuestions()[0].getResponses()).hasSize(4)
    assertThat(report.history).hasSize(3)
    assertThat(report.history[2].questions).hasSize(3)
    assertThat(report.history[2].questions[1].getResponses()).hasSize(2)
    assertThat(report.history[2].questions[1].getResponses()[1].response).isEqualTo("OTHER")
  }
}
