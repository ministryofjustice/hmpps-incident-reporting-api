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
            summary = "An event summary",
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
    incidentReport.addStaffInvolved(StaffRole.REPORTER, "user1")
    incidentReport.addPrisonerInvolved("A1234AA", PrisonerRole.WITNESS)
    incidentReport.addOtherPersonInvolved("name1", PersonRole.WITNESS)
    incidentReport.addIncidentLocation("MDI-1-1-1", "CELL", "Other stuff")
    incidentReport.addDataPoint("WHERE_OCCUR", "DETOX_UNIT", "user1", LocalDateTime.now())
      .addDataPointValue("HEALTH_CARE_CENTRE", "some info")
    incidentReport.addDataPoint("CELL_TYPE", "ORDINARY", "user1", LocalDateTime.now())

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val ir1 = repository.findOneByIncidentNumber(incidentReport.incidentNumber)
    assertThat(ir1).isNotNull
  }
}
