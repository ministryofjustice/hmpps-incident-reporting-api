package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.OrderColumn
import org.hibernate.annotations.GenericGenerator
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisCorrectionRequests
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisHistory
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisPrisonerInvolvements
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisQuestions
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisStaffInvolvements
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.id.UuidV7Generator
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report as ReportDto

@Entity
class Report(
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", type = UuidV7Generator::class)
  @Column(name = "id", updatable = false, nullable = false)
  val id: UUID? = null,

  /**
   * Human readable ID.
   * A number when sourced from NOMIS.
   * Prefixed with “IR-” when sourced from DPS.
   */
  @Column(nullable = false, unique = true, length = 25)
  val incidentNumber: String,

  var incidentDateAndTime: LocalDateTime,

  var prisonId: String,

  @Enumerated(EnumType.STRING)
  var type: Type,

  @Enumerated(EnumType.STRING)
  var status: Status = Status.DRAFT,

  var title: String,
  var description: String,

  var reportedBy: String,
  var reportedDate: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], optional = false)
  val event: Event,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val historyOfStatuses: MutableList<StatusHistory> = mutableListOf(),

  // TODO: what's this for?
  val assignedTo: String,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val staffInvolved: MutableList<StaffInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val prisonersInvolved: MutableList<PrisonerInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val locations: MutableList<Location> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val evidence: MutableList<Evidence> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val correctionRequests: MutableList<CorrectionRequest> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence", nullable = false)
  private val questions: MutableList<Question> = mutableListOf(),

  var questionSetId: String? = null,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("change_date ASC")
  val history: MutableList<History> = mutableListOf(),

  @Enumerated(EnumType.STRING)
  val source: InformationSource = InformationSource.DPS,

  val createdDate: LocalDateTime,
  var lastModifiedDate: LocalDateTime,
  var lastModifiedBy: String,
) {
  override fun toString(): String {
    return "Report(incidentNumber=$incidentNumber)"
  }

  fun getQuestions(): List<Question> = questions

  fun changeType(newType: Type, changedDate: LocalDateTime, staffChanged: String): Report {
    copyToHistory(changedDate, staffChanged)
    questions.clear()
    type = newType
    return this
  }

  fun addStatusHistory(status: Status, setOn: LocalDateTime, setBy: String): StatusHistory {
    return StatusHistory(
      report = this,
      status = status,
      setOn = setOn,
      setBy = setBy,
    ).also { historyOfStatuses.add(it) }
  }

  fun addEvidence(type: String, description: String): Evidence {
    return Evidence(
      report = this,
      type = type,
      description = description,
    ).also { evidence.add(it) }
  }

  fun addStaffInvolved(staffRole: StaffRole, username: String, comment: String? = null): StaffInvolvement {
    return StaffInvolvement(
      report = this,
      staffUsername = username,
      staffRole = staffRole,
      comment = comment,
    ).also { staffInvolved.add(it) }
  }

  fun addPrisonerInvolved(
    prisonerNumber: String,
    prisonerRole: PrisonerRole,
    prisonerOutcome: PrisonerOutcome? = null,
    comment: String? = null,
  ): PrisonerInvolvement {
    return PrisonerInvolvement(
      report = this,
      prisonerNumber = prisonerNumber,
      prisonerRole = prisonerRole,
      outcome = prisonerOutcome,
      comment = comment,
    ).also { prisonersInvolved.add(it) }
  }

  fun addLocation(
    locationId: String,
    locationType: String,
    description: String,
  ): Location {
    return Location(
      report = this,
      locationId = locationId,
      type = locationType,
      description = description,
    ).also { locations.add(it) }
  }

  fun addCorrectionRequest(
    correctionRequestedBy: String,
    correctionRequestedAt: LocalDateTime,
    reason: CorrectionReason,
    descriptionOfChange: String,
  ): CorrectionRequest {
    return CorrectionRequest(
      report = this,
      correctionRequestedBy = correctionRequestedBy,
      correctionRequestedAt = correctionRequestedAt,
      reason = reason,
      descriptionOfChange = descriptionOfChange,
    ).also { correctionRequests.add(it) }
  }

  fun addQuestion(
    code: String,
    question: String,
    additionalInformation: String? = null,
  ): Question {
    return Question(
      report = this,
      code = code,
      question = question,
      additionalInformation = additionalInformation,
    ).also { questions.add(it) }
  }

  fun addHistory(type: Type, incidentChangeDate: LocalDateTime, staffChanged: String): History {
    return History(
      report = this,
      type = type,
      changeDate = incidentChangeDate,
      changeStaffUsername = staffChanged,
    ).also { history.add(it) }
  }

  private fun copyToHistory(changedDate: LocalDateTime, staffChanged: String): History {
    val history = addHistory(type, changedDate, staffChanged)
    getQuestions().filterNotNull().forEach { question ->
      val historicalQuestion = history.addQuestion(
        code = question.code,
        question = question.question,
        additionalInformation = question.additionalInformation,
      )
      question.getResponses().forEach { response ->
        historicalQuestion.addResponse(
          response.response,
          response.additionalInformation,
          response.recordedBy,
          response.recordedOn,
        )
      }
    }
    return history
  }

  fun updateWith(upsert: NomisReport, updatedBy: String, clock: Clock) {
    val now = LocalDateTime.now(clock)

    type = Type.fromNomisCode(upsert.type)

    // NOTE: Currently we update the event information as well on update.
    //       For some of these fields makes more sense because that's explicitly
    //       the intent (e.g. `incidentDateTime`, `prisonId`, etc... are also in the event)
    //       For Event's title/description may make less sense but we're keeping this in
    //       as well for now.
    incidentDateAndTime = upsert.incidentDateTime
    event.eventDateAndTime = incidentDateAndTime

    prisonId = upsert.prison.code
    event.prisonId = prisonId

    title = upsert.title ?: "NO DETAILS GIVEN"
    event.title = title

    description = upsert.description ?: "NO DETAILS GIVEN"
    event.description = description

    reportedBy = upsert.reportingStaff.username
    reportedDate = upsert.reportedDateTime

    val newStatus = Status.fromNomisCode(upsert.status.code)
    if (newStatus != status) {
      status = newStatus
      addStatusHistory(newStatus, now, updatedBy)
    }

    questionSetId = "${upsert.questionnaireId}"

    lastModifiedDate = now
    event.lastModifiedDate = lastModifiedDate

    lastModifiedBy = updatedBy
    event.lastModifiedBy = updatedBy

    staffInvolved.clear()
    addNomisStaffInvolvements(upsert.staffParties)

    prisonersInvolved.clear()
    addNomisPrisonerInvolvements(upsert.offenderParties)

    correctionRequests.clear()
    addNomisCorrectionRequests(upsert.requirements)

    questions.clear()
    addNomisQuestions(upsert.questions)

    history.clear()
    addNomisHistory(upsert.history)
  }

  fun toDto() = ReportDto(
    id = id!!,
    incidentNumber = incidentNumber,
    incidentDateAndTime = incidentDateAndTime,
    prisonId = prisonId,
    type = type,
    title = title,
    description = description,
    reportedBy = reportedBy,
    reportedDate = reportedDate,
    status = status,
    assignedTo = assignedTo,
    createdDate = createdDate,
    lastModifiedDate = lastModifiedDate,
    lastModifiedBy = lastModifiedBy,
    createdInNomis = source == InformationSource.NOMIS,
    event = event.toDto(),
    questions = questions.map { it.toDto() },
    history = history.map { it.toDto() },
    historyOfStatuses = historyOfStatuses.map { it.toDto() },
    staffInvolved = staffInvolved.map { it.toDto() },
    prisonersInvolved = prisonersInvolved.map { it.toDto() },
    locations = locations.map { it.toDto() },
    evidence = evidence.map { it.toDto() },
    correctionRequests = correctionRequests.map { it.toDto() },
  )
}
