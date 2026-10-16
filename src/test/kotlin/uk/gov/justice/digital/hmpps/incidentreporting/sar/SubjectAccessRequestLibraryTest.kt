package uk.gov.justice.digital.hmpps.incidentreporting.sar

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserType
import uk.gov.justice.digital.hmpps.incidentreporting.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * The four checks from the HMPPS SAR test library, against a fixed set of incident reports.
 *
 * These are approval tests: each compares against a committed file under `src/test/resources/sar/`, and the
 * point of them is what happens when one fails.
 *
 *  - `SarApiDataTest` / `SarReportTest` fail when the response or the rendered report changes. The rendered
 *    HTML is the artefact the Offender SAR team review and sign off (IR-1461), and no template change reaches
 *    preprod or prod without that sign-off. So a diff here is the trigger for a conversation with them through the
 *    SAR change control process, not something to regenerate and move on from. `SarReportTest` renders the
 *    template served by `GET /subject-access-request/template`, so it also proves the endpoint is switched on.
 *  - `SarJpaEntitiesTest` fails when a column is added to any entity, and `SarFlywaySchemaTest` when a
 *    migration is added. Both force whoever makes the change to decide whether it belongs in a prisoner's
 *    report - and to tag the new column in the SAR data requirements extract - rather than the field quietly
 *    never appearing.
 *
 * Regenerate the files by running with `SAR_GENERATE_ACTUAL=true`, which writes `*.log` files under
 * `src/test/resources/`, then read them before copying them over the approved ones.
 */
@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestLibraryTest :
  SqsIntegrationTestBase(),
  SarApiDataTest,
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarReportTest {

  @Autowired
  private lateinit var reportRepository: ReportRepository

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String = PRISONER

  /**
   * Deserialise the response as plain JSON rather than into the response DTOs. That is what the SAR tool itself
   * does - it fetches the JSON and renders the template against the parsed result - so the template helpers see
   * the same strings they see in production.
   */
  override fun getContentType(): Class<*> = List::class.java

  /**
   * The test library asks for "a complete representation of all possible data", because anything not
   * represented here is a shape of report nobody has ever looked at. Between them these reports exercise every
   * field in the response and every branch in the template:
   *  - a DPS report with everything: two prisoners (one of them the requester, whose prison number must not be
   *    printed), two staff, an addition to the description, questions with several answers, a date and further
   *    information, a correction request, a status trail, and an earlier incident type with its own answers;
   *  - an older NOMIS report with only the minimum, marked as a duplicate, so every empty section shows;
   *  - a report the requester is not on, which must not appear at all.
   *
   * The legacy `question` and `response` columns are deliberately given different wording from the labels, so
   * the approved report shows it is the label - the wording the user saw - that is printed.
   *
   * Dates are fixed so the approved files stay stable. Idempotent, because the library calls this itself and the
   * reports must not stack up.
   */
  override fun setupTestData() {
    reportRepository.deleteAll()
    reportRepository.saveAll(listOf(fullReport(), duplicateNomisReport(), reportWithoutRequester()))
  }

  /** Reported as self-harm, changed to an assault, sent back for more detail and updated. */
  private fun fullReport(): Report {
    val report = Report(
      reportReference = "1000101",
      incidentDateAndTime = LocalDateTime.parse("2025-03-14T09:30:00"),
      location = "MDI",
      type = Type.ASSAULT_5,
      status = Status.UPDATED,
      title = "Assault: Smith A1234SR, Jones B2345SR (Moorland (HMP & YOI))",
      description = "Officer found the prisoner in their cell with a cut to the forearm. Healthcare attended.",
      reportedBy = "SAR_USER1",
      reportedAt = LocalDateTime.parse("2025-03-14T11:05:00"),
      createdAt = LocalDateTime.parse("2025-03-14T11:05:00"),
      modifiedAt = LocalDateTime.parse("2025-03-16T09:00:00"),
      modifiedBy = "SAR_USER1",
    )
    report.addStatusHistory(Status.DRAFT, LocalDateTime.parse("2025-03-14T11:05:00"), "SAR_USER1")
    report.addStatusHistory(Status.AWAITING_REVIEW, LocalDateTime.parse("2025-03-14T11:20:00"), "SAR_USER1")
    report.addStatusHistory(Status.NEEDS_UPDATING, LocalDateTime.parse("2025-03-15T14:20:00"), "SAR_WARDEN")
    report.addStatusHistory(Status.UPDATED, LocalDateTime.parse("2025-03-16T09:00:00"), "SAR_USER1")

    report.addDescriptionAddendum(
      sequence = 0,
      createdBy = "SAR_USER2",
      createdAt = LocalDateTime.parse("2025-03-16T08:55:00"),
      firstName = "Alex",
      lastName = "Brown",
      text = "The other prisoner was seen leaving the cell shortly before.",
    )

    report.addPrisonerInvolved(
      sequence = 0,
      prisonerNumber = PRISONER,
      firstName = "Sam",
      lastName = "Smith",
      prisonerRole = PrisonerRole.VICTIM,
      outcome = PrisonerOutcome.SEEN_HEALTHCARE,
      comment = "Cut to the forearm, cleaned and dressed.",
    )
    report.addPrisonerInvolved(
      sequence = 1,
      prisonerNumber = OTHER_PRISONER,
      firstName = "Joe",
      lastName = "Jones",
      prisonerRole = PrisonerRole.SUSPECTED_ASSAILANT,
      outcome = PrisonerOutcome.PLACED_ON_REPORT,
    )

    report.addStaffInvolved(
      sequence = 0,
      staffRole = StaffRole.FIRST_ON_SCENE,
      staffUsername = "SAR_USER1",
      firstName = "Jane",
      lastName = "Doe",
      comment = "First to arrive and called for healthcare.",
    )
    report.addStaffInvolved(
      sequence = 1,
      staffRole = StaffRole.WITNESS,
      staffUsername = null,
      firstName = "Pat",
      lastName = "Green",
    )

    report.addQuestion(code = "61279", question = "WAS A WEAPON USED", label = "Was a weapon used?", sequence = 0)
      .addResponse(
        code = "61280",
        response = "NO",
        label = "No",
        sequence = 0,
        recordedBy = "SAR_USER1",
        recordedAt = LocalDateTime.parse("2025-03-14T11:10:00"),
      )
    report.addQuestion(
      code = "61281",
      question = "INJURIES SUSTAINED",
      label = "What injuries were sustained?",
      sequence = 1,
      additionalInformation = "Injuries were photographed.",
    ).apply {
      addResponse(
        code = "61282",
        response = "CUT",
        label = "Cut",
        sequence = 0,
        additionalInformation = "Cut to the left forearm.",
        recordedBy = "SAR_USER1",
        recordedAt = LocalDateTime.parse("2025-03-14T11:10:00"),
      )
      addResponse(
        code = "61283",
        response = "BRUISING",
        label = "Bruising",
        sequence = 1,
        recordedBy = "SAR_USER1",
        recordedAt = LocalDateTime.parse("2025-03-14T11:10:00"),
      )
    }
    report.addQuestion(
      code = "61284",
      question = "DATE SEEN BY HEALTHCARE",
      label = "When was the prisoner seen by healthcare?",
      sequence = 2,
    ).addResponse(
      code = "61285",
      response = "DATE",
      label = "Date seen",
      responseDate = LocalDate.parse("2025-03-14"),
      sequence = 0,
      recordedBy = "SAR_USER1",
      recordedAt = LocalDateTime.parse("2025-03-16T09:00:00"),
    )

    report.addCorrectionRequest(
      sequence = 0,
      correctionRequestedBy = "SAR_WARDEN",
      correctionRequestedAt = LocalDateTime.parse("2025-03-15T14:20:00"),
      descriptionOfChange = "Please add the names of the staff who attended.",
      location = "MDI",
      userAction = UserAction.REQUEST_CORRECTION,
      userType = UserType.DATA_WARDEN,
    )

    report.addHistory(Type.SELF_HARM_1, LocalDateTime.parse("2025-03-14T11:15:00"), "SAR_USER1")
      .addQuestion(
        code = "44155",
        question = "TREATMENT REQUIRED FOLLOWING CUT SCRATCH",
        label = "Treatment required following cut scratch",
        sequence = 0,
      )
      .addResponse(
        code = "179011",
        response = "CLEANED AND DRESSED",
        label = "Cleaned and dressed",
        sequence = 0,
        recordedBy = "SAR_USER1",
        recordedAt = LocalDateTime.parse("2025-03-14T11:10:00"),
      )

    return report
  }

  /** Created in NOMIS and marked as a duplicate. Nothing optional is recorded, so every empty section shows. */
  private fun duplicateNomisReport(): Report {
    val report = Report(
      reportReference = "1000050",
      incidentDateAndTime = LocalDateTime.parse("2024-11-02T20:15:00"),
      location = "LEI",
      type = Type.FIND_6,
      status = Status.DUPLICATE,
      source = InformationSource.NOMIS,
      modifiedIn = InformationSource.NOMIS,
      title = "Find of illicit items",
      description = "Search of cell found a mobile phone.",
      reportedBy = "SAR_USER2",
      reportedAt = LocalDateTime.parse("2024-11-02T21:00:00"),
      createdAt = LocalDateTime.parse("2024-11-02T21:00:00"),
      modifiedAt = LocalDateTime.parse("2024-11-04T10:00:00"),
      modifiedBy = "SAR_WARDEN",
    )
    report.addStatusHistory(Status.AWAITING_REVIEW, LocalDateTime.parse("2024-11-02T21:00:00"), "SAR_USER2")
    report.addStatusHistory(Status.DUPLICATE, LocalDateTime.parse("2024-11-04T10:00:00"), "SAR_WARDEN")
    report.addPrisonerInvolved(
      sequence = 0,
      prisonerNumber = PRISONER,
      firstName = "Sam",
      lastName = "Smith",
      prisonerRole = PrisonerRole.IN_POSSESSION,
    )
    report.addCorrectionRequest(
      sequence = 0,
      correctionRequestedBy = "SAR_USER2",
      correctionRequestedAt = LocalDateTime.parse("2024-11-03T08:30:00"),
      descriptionOfChange = "Already reported by another officer.",
      userAction = UserAction.REQUEST_DUPLICATE,
      originalReportReference = "1000049",
      userType = UserType.REPORTING_OFFICER,
    )
    return report
  }

  /** Names only another prisoner, so it must not appear in the requester's report. */
  private fun reportWithoutRequester(): Report {
    val report = Report(
      reportReference = "1000102",
      incidentDateAndTime = LocalDateTime.parse("2025-03-20T10:00:00"),
      location = "MDI",
      type = Type.SELF_HARM_1,
      status = Status.AWAITING_REVIEW,
      title = "Self-harm: Jones B2345SR (Moorland (HMP & YOI))",
      description = "Unrelated incident.",
      reportedBy = "SAR_USER1",
      reportedAt = LocalDateTime.parse("2025-03-20T10:30:00"),
      createdAt = LocalDateTime.parse("2025-03-20T10:30:00"),
      modifiedAt = LocalDateTime.parse("2025-03-20T10:30:00"),
      modifiedBy = "SAR_USER1",
    )
    report.addPrisonerInvolved(
      sequence = 0,
      prisonerNumber = OTHER_PRISONER,
      firstName = "Joe",
      lastName = "Jones",
      prisonerRole = PrisonerRole.ACTIVE_INVOLVEMENT,
    )
    return report
  }

  private companion object {
    const val PRISONER = "A1234SR"
    const val OTHER_PRISONER = "B2345SR"
  }
}
