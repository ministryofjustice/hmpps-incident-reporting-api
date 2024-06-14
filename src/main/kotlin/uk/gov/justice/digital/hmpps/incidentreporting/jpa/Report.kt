package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedEntityGraphs
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.OrderColumn
import org.hibernate.annotations.BatchSize
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.NO_DETAILS_GIVEN
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisCorrectionRequests
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisHistory
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisPrisonerInvolvements
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisQuestions
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.addNomisStaffInvolvements
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.id.GeneratedUuidV7
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Entity
@NamedEntityGraphs(
  value = [
    NamedEntityGraph(
      name = "Report.eager",
      attributeNodes = [
        NamedAttributeNode("event"),
        NamedAttributeNode("questions", subgraph = "Report.eager.subgraph"),
      ],
      subgraphs = [
        NamedSubgraph(
          name = "Report.eager.subgraph",
          attributeNodes = [
            NamedAttributeNode("responses"),
          ],
        ),
      ],
    ),
  ],
)
class Report(
  @Id
  @GeneratedUuidV7
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

  @Enumerated(EnumType.STRING)
  val source: InformationSource = InformationSource.DPS,

  var title: String,
  var description: String,

  var reportedBy: String,
  var reportedAt: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH], optional = false)
  var event: Event,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("changed_at ASC")
  @BatchSize(size = 10)
  val historyOfStatuses: MutableList<StatusHistory> = mutableListOf(),

  // TODO: what's this for?
  val assignedTo: String,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  @BatchSize(size = 10)
  val staffInvolved: MutableList<StaffInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  @BatchSize(size = 10)
  val prisonersInvolved: MutableList<PrisonerInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  @BatchSize(size = 10)
  val locations: MutableList<Location> = mutableListOf(),

  // TODO: what's this for?
  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  @BatchSize(size = 10)
  val evidence: MutableList<Evidence> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  @BatchSize(size = 10)
  val correctionRequests: MutableList<CorrectionRequest> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence", nullable = false)
  @BatchSize(size = 50)
  private val questions: MutableList<Question> = mutableListOf(),

  var questionSetId: String? = null,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("changed_at ASC")
  @BatchSize(size = 10)
  val history: MutableList<History> = mutableListOf(),

  var createdAt: LocalDateTime,
  var modifiedAt: LocalDateTime,
  var modifiedBy: String,
) {
  override fun toString(): String {
    return "Report(incidentNumber=$incidentNumber)"
  }

  fun getQuestions(): List<Question> = questions

  fun changeType(newType: Type, changedAt: LocalDateTime, changedBy: String): Report {
    copyToHistory(changedAt, changedBy)
    questions.clear()
    type = newType
    return this
  }

  fun addStatusHistory(status: Status, changedAt: LocalDateTime, changedBy: String): StatusHistory {
    return StatusHistory(
      report = this,
      status = status,
      changedAt = changedAt,
      changedBy = changedBy,
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

  fun addHistory(type: Type, changedAt: LocalDateTime, changedBy: String): History {
    return History(
      report = this,
      type = type,
      changedAt = changedAt,
      changedBy = changedBy,
    ).also { history.add(it) }
  }

  private fun copyToHistory(changedAt: LocalDateTime, changedBy: String): History {
    val history = addHistory(type, changedAt, changedBy)
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
          response.recordedAt,
        )
      }
    }
    return history
  }

  fun updateWith(upsert: NomisReport, clock: Clock) {
    val updatedBy = upsert.lastModifiedBy ?: upsert.createdBy
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

    title = upsert.title ?: NO_DETAILS_GIVEN
    event.title = title

    description = upsert.description ?: NO_DETAILS_GIVEN
    event.description = description

    reportedBy = upsert.reportingStaff.username
    reportedAt = upsert.reportedDateTime

    val newStatus = Status.fromNomisCode(upsert.status.code)
    if (newStatus != status) {
      status = newStatus
      addStatusHistory(newStatus, now, updatedBy)
    }

    questionSetId = "${upsert.questionnaireId}"

    createdAt = upsert.createDateTime
    event.createdAt = upsert.createDateTime

    modifiedAt = upsert.lastModifiedDateTime ?: upsert.createDateTime
    event.modifiedAt = modifiedAt

    modifiedBy = updatedBy
    event.modifiedBy = updatedBy

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

  fun toDtoBasic() = ReportBasic(
    id = id!!,
    incidentNumber = incidentNumber,
    incidentDateAndTime = incidentDateAndTime,
    prisonId = prisonId,
    type = type,
    title = title,
    description = description,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
    status = status,
    assignedTo = assignedTo,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    createdInNomis = source == InformationSource.NOMIS,
  )

  fun toDtoWithDetails() = ReportWithDetails(
    id = id!!,
    incidentNumber = incidentNumber,
    incidentDateAndTime = incidentDateAndTime,
    prisonId = prisonId,
    type = type,
    title = title,
    description = description,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
    status = status,
    assignedTo = assignedTo,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
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
