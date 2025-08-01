package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.helper.buildReport
import uk.gov.justice.digital.hmpps.incidentreporting.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.AnalysisMarkerRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository

private const val MEDICAL_YES = "YES_MEDICAL_TREATMENT_REQUIRED"
private const val SERIOUS_INJURY_SUSTAINED_YES = "SERIOUS_INJURY_SUSTAINED_YES"
private const val REPORT_REFERENCE = "12345"

@DisplayName("Test persistence of analytical markers")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnalyticalMarkerRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var analysisMarkerRepository: AnalysisMarkerRepository

  @Autowired
  lateinit var reportRepository: ReportRepository

  @BeforeEach
  fun setUp() {
    reportRepository.deleteAll()
    analysisMarkerRepository.deleteAll()

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()
  }

  @Test
  fun `can add markers to a report`() {
    analysisMarkerRepository.saveAll(
      listOf(
        AnalyticalMarker(
          AnalyticalMarkerPk(
            responseCode = SERIOUS_INJURY_SUSTAINED_YES,
            markerType = AnalyticalMarkerType.SERIOUS_INJURY,
          ),
        ),
        AnalyticalMarker(
          AnalyticalMarkerPk(
            responseCode = MEDICAL_YES,
            markerType = AnalyticalMarkerType.HOSPITAL_ADMISSION,
          ),
        ),
        AnalyticalMarker(
          AnalyticalMarkerPk(
            responseCode = MEDICAL_YES,
            markerType = AnalyticalMarkerType.MEDICAL_TREATMENT_REQUIRED,
          ),
        ),
      ),
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    val report = reportRepository.save(
      buildReport(
        reportReference = REPORT_REFERENCE,
        reportTime = now.minusDays(1),
        type = Type.ASSAULT_5,
      ),
    )

    report
      .addQuestion(
        code = "INJURY_SUSTAINED",
        question = "WAS A SERIOUS INJURY SUSTAINED",
        label = "Was a serious injury sustained?",
        1,
      ).addResponse(
        code = SERIOUS_INJURY_SUSTAINED_YES,
        response = "YES",
        label = "Yes",
        sequence = 0,
        recordedBy = "staff-1",
        recordedAt = now,
      )
    report.addQuestion(
      code = "MEDICAL_TREATMENT_REQUIRED",
      question = "WAS MEDICAL TREATMENT FOR CONCUSSION OR INTERNAL INJURIES REQUIRED",
      label = "Was medical treatment required?",
      2,
    ).addResponse(
      code = MEDICAL_YES,
      response = "YES",
      label = "Yes",
      sequence = 0,
      recordedBy = "staff-1",
      recordedAt = now,
    )

    TestTransaction.flagForCommit()
    TestTransaction.end()
    TestTransaction.start()

    assertThat(
      analysisMarkerRepository.findByIdResponseCode(MEDICAL_YES)
        .map { it.id.markerType },
    )
      .containsExactly(
        AnalyticalMarkerType.HOSPITAL_ADMISSION,
        AnalyticalMarkerType.MEDICAL_TREATMENT_REQUIRED,
      )

    // check we can find reports by the marker data that has been created
    assertThat(reportRepository.findAllByAnalyticalMarker(AnalyticalMarkerType.HOSPITAL_ADMISSION)).isNotEmpty
    assertThat(reportRepository.findAllByAnalyticalMarker(AnalyticalMarkerType.SEXUAL_ASSAULT)).isEmpty()
  }
}
