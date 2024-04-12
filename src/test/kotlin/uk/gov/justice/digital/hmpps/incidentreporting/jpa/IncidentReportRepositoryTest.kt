package uk.gov.justice.digital.hmpps.incidentreporting.jpa

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
    val incidentReport =
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

    incidentReport.addIncidentData("WHERE_OCCURRED", "Where did this occur?")
      .addDataItem("DETOX_UNIT", "They hurt themselves", "user1", now)
      .addDataItem("CELL", "In the cell", "user1", now)

    incidentReport.addIncidentData("METHOD", "Method Used to hurt themselves?")
      .addDataItem("KNIFE", "They used a knife", "user1", now)
      .addDataItem("OTHER", "They used something else", "user1", now)

    val before1 = now.minusMinutes(5)
    incidentReport.addIncidentHistory(IncidentType.FINDS, before1, "user2")
      .addHistoricalResponse("dataItem3", "dataItemDescription3")
      .addDataItem("response1", "Some information", "user1", before1)
      .addDataItem("response2", "Some information", "user1", before1)
      .addDataItem("response3", "Some information", "user1", before1)

    val before2 = now.minusMinutes(2)
    incidentReport.addIncidentHistory(IncidentType.ASSAULT, before2, "user1")
      .addHistoricalResponse("dataItem1", "dataItemDescription1")
      .addDataItem("response1", "Some information", "user1", before2)
      .addDataItem("response2", "Some information", "user1", before2)

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val ir1 = reportRepository.findOneByIncidentNumber(incidentReport.incidentNumber)
    assertThat(ir1).isNotNull
  }
}
