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
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class IncidentReportRepositoryTest : IntegrationTestBase() {

  @Autowired
  lateinit var repository: IncidentReportRepository

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Test
  fun `Create an incident report`() {
    val incidentReport =
      repository.save(
        IncidentReport(
          incidentDateAndTime = LocalDateTime.now(),
          incidentNumber = "A1100011",
          incidentType = IncidentType.SELF_HARM,
          incidentDetails = "An incident occurred",
          reportedBy = "user1",
          prisonId = "MDI",
          reportedDate = LocalDateTime.now(),
          assignedTo = "user2",
          createdDate = LocalDateTime.now(),
          lastModifiedDate = LocalDateTime.now(),
          lastModifiedBy = "user1",
          questionSetUsed = QuestionSet.SELF_HARM_V1,
        ),
      )

    incidentReport.addEvidence("evidence2", "description2")
    incidentReport.addStaffInvolved(StaffRole.REPORTER, "user1")
    incidentReport.addPrisonerInvolved("A1234AA", PrisonerRole.WITNESS)
    incidentReport.addOtherPersonInvolved("name1", PersonRole.WITNESS)
    incidentReport.addIncidentLocation("MDI-1-1-1", "CELL", "Other stuff")
    incidentReport.addResponse(Question.WHERE_DID_THE_INCIDENT_OCCUR, ResponseOption.DETOX_UNIT, "user1", LocalDateTime.now())
      .addResponse(ResponseOption.HEALTH_CARE_CENTRE, "some info")
    incidentReport.addResponse(Question.WHAT_WAS_THE_CELL_TYPE, ResponseOption.ORDINARY, "user1", LocalDateTime.now())

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val ir1 = repository.findOneByIncidentNumber(incidentReport.incidentNumber)
    assertThat(ir1).isNotNull
  }
}
