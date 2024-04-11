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
class IncidentReportReportRepositoryTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: IncidentReportRepository

  @Autowired
  lateinit var eventRepository: IncidentEventRepository

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Test
  fun `Create an incident report`() {
    val incidentReport =
      repository.save(
        IncidentReport(
          incidentNumber = repository.generateIncidentReportNumber(),
          incidentDateAndTime = LocalDateTime.now(),
          incidentType = IncidentType.SELF_HARM,
          summary = "A Summary",
          incidentDetails = "An incident occurred",
          reportedBy = "user1",
          prisonId = "MDI",
          event = IncidentEvent(
            eventId = eventRepository.generateEventId(),
            eventDateAndTime = LocalDateTime.now(),
            prisonId = "MDI",
            eventDetails = "An event occurred",
            createdDate = LocalDateTime.now(),
            lastModifiedDate = LocalDateTime.now(),
            lastModifiedBy = "user1",
          ),
          reportedDate = LocalDateTime.now(),
          assignedTo = "user2",
          createdDate = LocalDateTime.now(),
          lastModifiedDate = LocalDateTime.now(),
          lastModifiedBy = "user1",
        ),
      )

    incidentReport.addEvidence("evidence2", "description2")
    incidentReport.addStaffInvolved(StaffRole.FIRST_ON_SCENE, "user1")
    incidentReport.addPrisonerInvolved("A1234AA", PrisonerRole.VICTIM)
    incidentReport.addIncidentLocation("MDI-1-1-1", "CELL", "Other stuff")

    incidentReport.addIncidentData("WHERE_OCCURRED", "Where did this occur?")
      .addDataItem("DETOX_UNIT", "They hurt themselves", "user1", LocalDateTime.now())
      .addDataItem("CELL", "In the cell", "user1", LocalDateTime.now())

    incidentReport.addIncidentData("METHOD", "Method Used to hurt themselves?")
      .addDataItem("KNIFE", "They used a knife", "user1", LocalDateTime.now())
      .addDataItem("OTHER", "They used something else", "user1", LocalDateTime.now())

    incidentReport.addIncidentHistory(IncidentType.FINDS, LocalDateTime.now().minusHours(1), "user2")
      .addHistoricalResponse("dataItem3", "dataItemDescription3")
      .addDataItem("response1", "Some information", "user1", LocalDateTime.now())
      .addDataItem("response2", "Some information", "user1", LocalDateTime.now())
      .addDataItem("response3", "Some information", "user1", LocalDateTime.now())

    incidentReport.addIncidentHistory(IncidentType.ASSAULT, LocalDateTime.now(), "user1")
      .addHistoricalResponse("dataItem1", "dataItemDescription1")
      .addDataItem("response1", "Some information", "user1", LocalDateTime.now())
      .addDataItem("response2", "Some information", "user1", LocalDateTime.now())

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val ir1 = repository.findOneByIncidentNumber(incidentReport.incidentNumber)
    assertThat(ir1).isNotNull
  }
}
