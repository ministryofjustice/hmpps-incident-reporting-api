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
import org.hibernate.Hibernate
import org.hibernate.annotations.SortNatural
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.NO_DETAILS_GIVEN
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistory
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisOffenderParty
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisRequirement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisStaffParty
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.id.GeneratedUuidV7
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ObjectAtIndexNotFoundException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.QuestionsNotFoundException
import java.time.Clock
import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

@Entity
@NamedEntityGraphs(
  value = [
    NamedEntityGraph(
      name = "Report.eager",
      attributeNodes = [
        NamedAttributeNode("event"),
        NamedAttributeNode("staffInvolved"),
        NamedAttributeNode("prisonersInvolved"),
        NamedAttributeNode("correctionRequests"),
        NamedAttributeNode("historyOfStatuses"),
        NamedAttributeNode("questions", subgraph = "questions.eager.subgraph"),
        NamedAttributeNode("history", subgraph = "history.eager.subgraph"),
      ],
      subgraphs = [
        NamedSubgraph(
          name = "questions.eager.subgraph",
          attributeNodes = [
            NamedAttributeNode("responses"),
          ],
        ),
        NamedSubgraph(
          name = "history.eager.subgraph",
          attributeNodes = [
            NamedAttributeNode("questions", subgraph = "history.responses.eager.subgraph"),
          ],
        ),
        NamedSubgraph(
          name = "history.responses.eager.subgraph",
          attributeNodes = [
            NamedAttributeNode("responses"),
          ],
        ),
      ],
    ),
  ],
)
@EntityOpen
class Report(
  /**
   * Internal ID which should not be seen by users
   */
  @Id
  @GeneratedUuidV7
  @Column(name = "id", updatable = false, nullable = false)
  var id: UUID? = null,

  /**
   * Human-readable reference. Previously known as "incident number" in NOMIS.
   */
  @Column(nullable = false, unique = true, length = 25)
  val reportReference: String,

  var incidentDateAndTime: LocalDateTime,

  var location: String,

  @Enumerated(EnumType.STRING)
  var type: Type,

  @Enumerated(EnumType.STRING)
  var status: Status = Status.DRAFT,

  /** Which system the report was first created */
  @Enumerated(EnumType.STRING)
  val source: InformationSource = InformationSource.DPS,
  /** Which system the report was last modified in */
  @Enumerated(EnumType.STRING)
  var modifiedIn: InformationSource = InformationSource.DPS,

  var title: String,
  var description: String,

  var reportedBy: String,
  var reportedAt: LocalDateTime,

  @ManyToOne(
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH],
    optional = false,
  )
  var event: Event,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val historyOfStatuses: SortedSet<StatusHistory> = sortedSetOf(),

  // TODO: what's this for?
  val assignedTo: String,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val staffInvolved: SortedSet<StaffInvolvement> = sortedSetOf(),
  var staffInvolvementDone: Boolean = true,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val prisonersInvolved: SortedSet<PrisonerInvolvement> = sortedSetOf(),
  var prisonerInvolvementDone: Boolean = true,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val correctionRequests: SortedSet<CorrectionRequest> = sortedSetOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val questions: SortedSet<Question> = sortedSetOf(),

  var questionSetId: String? = null,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val history: SortedSet<History> = sortedSetOf(),

  var createdAt: LocalDateTime,
  var modifiedAt: LocalDateTime,
  var modifiedBy: String,
) : Comparable<Report> {

  companion object {
    private val COMPARATOR = compareBy<Report>
      { it.reportReference }
  }

  override fun compareTo(other: Report) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Report

    return reportReference == other.reportReference
  }

  override fun hashCode(): Int {
    return reportReference.hashCode()
  }

  override fun toString(): String {
    return "Report(id=$id, reportReference=$reportReference, type=$type, status=$status, location=$location)"
  }

  fun changeType(
    newType: Type,
    changedAt: LocalDateTime,
    changedBy: String,
  ): Report {
    copyToHistory(changedAt, changedBy)
    questions.clear()
    type = newType
    return this
  }

  fun changeStatus(
    newStatus: Status,
    changedAt: LocalDateTime,
    changedBy: String,
  ): Report {
    status = newStatus
    addStatusHistory(newStatus, changedAt, changedBy)
    return this
  }

  fun addStatusHistory(
    status: Status,
    changedAt: LocalDateTime,
    changedBy: String,
  ): StatusHistory {
    return StatusHistory(
      report = this,
      status = status,
      changedAt = changedAt,
      changedBy = changedBy,
    ).also { historyOfStatuses.add(it) }
  }

  fun findStaffInvolvedByIndex(index: Int): StaffInvolvement = staffInvolved.elementAtOrNull(index - 1)
    ?: throw ObjectAtIndexNotFoundException(StaffInvolvement::class, index)

  fun updateStaffInvolved(nomisStaffParties: Collection<NomisStaffParty>) {
    this.staffInvolved.retainAll(
      nomisStaffParties.map { staffParty ->
        val newStaff = createStaffInvolved(staffParty)
        this.staffInvolved.find { it == newStaff }?.apply {
          staffUsername = newStaff.staffUsername
          firstName = newStaff.firstName
          lastName = newStaff.lastName
          staffRole = newStaff.staffRole
          comment = newStaff.comment
        } ?: addStaffInvolved(newStaff)
      }.toSet(),
    )
  }

  fun createStaffInvolved(nomisStaffParty: NomisStaffParty): StaffInvolvement = StaffInvolvement(
    report = this,
    sequence = nomisStaffParty.sequence,
    staffUsername = nomisStaffParty.staff.username,
    firstName = nomisStaffParty.staff.firstName,
    lastName = nomisStaffParty.staff.lastName,
    staffRole = StaffRole.fromNomisCode(nomisStaffParty.role.code),
    comment = nomisStaffParty.comment,
  )

  fun addStaffInvolved(staffInvolvement: StaffInvolvement): StaffInvolvement {
    this.staffInvolved.add(staffInvolvement)
    return staffInvolvement
  }

  fun addStaffInvolved(
    sequence: Int,
    staffRole: StaffRole,
    staffUsername: String?,
    firstName: String,
    lastName: String,
    comment: String? = null,
  ): StaffInvolvement {
    return addStaffInvolved(
      StaffInvolvement(
        report = this,
        sequence = sequence,
        staffUsername = staffUsername,
        firstName = firstName,
        lastName = lastName,
        staffRole = staffRole,
        comment = comment,
      ),
    )
  }

  fun removeStaffInvolved(staffInvolved: StaffInvolvement) {
    this.staffInvolved.remove(staffInvolved)
  }

  fun findPrisonerInvolvedByIndex(index: Int): PrisonerInvolvement = prisonersInvolved.elementAtOrNull(index - 1)
    ?: throw ObjectAtIndexNotFoundException(PrisonerInvolvement::class, index)

  fun updatePrisonerInvolved(nomisOffenderParties: Collection<NomisOffenderParty>) {
    val newInvolvements = nomisOffenderParties.map { nomisOffenderParty ->
      val newPrisoner = createPrisonerInvolved(nomisOffenderParty)
      prisonersInvolved.find { it == newPrisoner }?.apply {
        prisonerNumber = newPrisoner.prisonerNumber
        firstName = newPrisoner.firstName
        lastName = newPrisoner.lastName
        prisonerRole = newPrisoner.prisonerRole
        outcome = newPrisoner.outcome
        comment = newPrisoner.comment
      } ?: addPrisonerInvolved(newPrisoner)
    }.toSet()

    this.prisonersInvolved.retainAll(newInvolvements)
  }

  fun createPrisonerInvolved(nomisOffenderParty: NomisOffenderParty): PrisonerInvolvement = PrisonerInvolvement(
    report = this,
    sequence = nomisOffenderParty.sequence,
    prisonerNumber = nomisOffenderParty.offender.offenderNo,
    firstName = nomisOffenderParty.offender.firstName,
    lastName = nomisOffenderParty.offender.lastName,
    prisonerRole = PrisonerRole.fromNomisCode(nomisOffenderParty.role.code),
    outcome = nomisOffenderParty.outcome?.let { prisonerOutcome ->
      PrisonerOutcome.fromNomisCode(prisonerOutcome.code)
    },
    comment = nomisOffenderParty.comment,
  )

  fun addPrisonerInvolved(prisonerInvolvement: PrisonerInvolvement): PrisonerInvolvement {
    this.prisonersInvolved.add(prisonerInvolvement)
    return prisonerInvolvement
  }

  fun addPrisonerInvolved(
    sequence: Int,
    prisonerNumber: String,
    firstName: String,
    lastName: String,
    prisonerRole: PrisonerRole,
    outcome: PrisonerOutcome? = null,
    comment: String? = null,
  ): PrisonerInvolvement {
    return addPrisonerInvolved(
      PrisonerInvolvement(
        report = this,
        sequence = sequence,
        prisonerNumber = prisonerNumber,
        firstName = firstName,
        lastName = lastName,
        prisonerRole = prisonerRole,
        outcome = outcome,
        comment = comment,
      ),
    )
  }

  fun removePrisonerInvolved(prisonerInvolved: PrisonerInvolvement) {
    prisonersInvolved.remove(prisonerInvolved)
  }

  fun findCorrectionRequestByIndex(index: Int): CorrectionRequest = correctionRequests.elementAtOrNull(index - 1)
    ?: throw ObjectAtIndexNotFoundException(CorrectionRequest::class, index)

  fun updateCorrectionRequests(nomisRequirements: Collection<NomisRequirement>) {
    this.correctionRequests.retainAll(
      nomisRequirements.map { nomisRequirement ->
        val newCorrection = createCorrectionRequest(nomisRequirement)
        this.correctionRequests.find { it == newCorrection }?.apply {
          correctionRequestedBy = newCorrection.correctionRequestedBy
          correctionRequestedAt = newCorrection.correctionRequestedAt
          descriptionOfChange = newCorrection.descriptionOfChange
        } ?: addCorrectionRequest(newCorrection)
      }.toSet(),
    )
  }

  fun createCorrectionRequest(nomisRequirement: NomisRequirement): CorrectionRequest = CorrectionRequest(
    report = this,
    sequence = nomisRequirement.sequence,
    correctionRequestedBy = nomisRequirement.staff.username,
    correctionRequestedAt = nomisRequirement.date.atStartOfDay(),
    descriptionOfChange = nomisRequirement.comment ?: NO_DETAILS_GIVEN,
  )

  fun addCorrectionRequest(correctionRequest: CorrectionRequest): CorrectionRequest {
    this.correctionRequests.add(correctionRequest)
    return correctionRequest
  }

  fun addCorrectionRequest(
    sequence: Int,
    correctionRequestedBy: String,
    correctionRequestedAt: LocalDateTime,
    descriptionOfChange: String,
  ): CorrectionRequest {
    return addCorrectionRequest(
      CorrectionRequest(
        report = this,
        sequence = sequence,
        correctionRequestedBy = correctionRequestedBy,
        correctionRequestedAt = correctionRequestedAt,
        descriptionOfChange = descriptionOfChange,
      ),
    )
  }

  fun removeCorrectionRequest(correctionRequest: CorrectionRequest) {
    correctionRequests.remove(correctionRequest)
  }

  fun findQuestion(code: String, sequence: Int): Question? =
    this.questions.firstOrNull { it.code == code && it.sequence == sequence }

  fun updateQuestionAndResponses(nomisQuestions: List<NomisQuestion>) {
    this.questions.retainAll(
      nomisQuestions.map { nomisQuestion ->
        val question = updateOrAddQuestion(nomisQuestion)
        question.updateResponses(nomisQuestion.answers)
        question
      }.toSet(),
    )
  }

  private fun updateOrAddQuestion(nomisQuestion: NomisQuestion): Question = findQuestion(
    code = nomisQuestion.questionId.toString(),
    sequence = nomisQuestion.sequence,
  )?.apply {
    question = nomisQuestion.question
  } ?: addQuestion(nomisQuestion).also { newQuestion ->
    questions.add(newQuestion)
  }

  fun addQuestion(
    code: String,
    question: String,
    sequence: Int,
    additionalInformation: String? = null,
  ): Question {
    return Question(
      report = this,
      code = code,
      question = question,
      sequence = sequence,
      additionalInformation = additionalInformation,
    ).also { questions.add(it) }
  }

  fun addQuestion(nomisQuestion: NomisQuestion): Question = this.addQuestion(
    code = nomisQuestion.questionId.toString(),
    sequence = nomisQuestion.sequence,
    question = nomisQuestion.question,
  )

  fun removeQuestion(question: Question) {
    questions.remove(question)
  }

  fun removeQuestions(questionCodes: Set<String>) {
    val questionsToRemove = questions.filter { questionCodes.contains(it.code) }
    val questionCodesFound = questionsToRemove.mapTo(mutableSetOf()) { it.code }
    val missingCodes = questionCodes - questionCodesFound
    if (missingCodes.isNotEmpty()) {
      throw QuestionsNotFoundException(missingCodes)
    }
    questionsToRemove.forEach {
      removeQuestion(it)
    }
  }

  fun findHistory(changedAt: LocalDateTime, type: Type): History? =
    this.history.firstOrNull { it.changedAt == changedAt && it.type == type }

  fun addHistory(history: History): History {
    this.history.add(history)
    return history
  }

  fun addHistory(
    type: Type,
    changedAt: LocalDateTime,
    changedBy: String,
  ): History {
    return addHistory(
      History(
        report = this,
        type = type,
        changedAt = changedAt,
        changedBy = changedBy,
      ),
    )
  }

  fun createHistory(nomisHistory: NomisHistory) = History(
    report = this,
    type = Type.fromNomisCode(nomisHistory.type),
    changedAt = nomisHistory.incidentChangeDateTime ?: nomisHistory.createDateTime,
    changedBy = nomisHistory.incidentChangeStaff.username,
  )

  fun updateHistory(nomisHistories: Collection<NomisHistory>) {
    this.history.retainAll(
      nomisHistories.map { nomisHistory ->
        val newHistory = createHistory(nomisHistory)
        val foundHistory = findHistory(
          changedAt = newHistory.changedAt,
          type = newHistory.type,
        ) ?: addHistory(newHistory)
        foundHistory.updateQuestionAndResponses(nomisHistory, this.reportedAt)
        foundHistory
      }.toSet(),
    )
  }

  private fun copyToHistory(changedAt: LocalDateTime, changedBy: String): History {
    val history = addHistory(type, changedAt, changedBy)
    questions.forEach { question ->
      val historicalQuestion = history.addQuestion(
        code = question.code,
        question = question.question,
        sequence = question.sequence,
        additionalInformation = question.additionalInformation,
      )
      question.responses.forEach { response ->
        historicalQuestion.addResponse(
          response = response.response,
          sequence = response.sequence,
          responseDate = response.responseDate,
          additionalInformation = response.additionalInformation,
          recordedBy = response.recordedBy,
          recordedAt = response.recordedAt,
        )
      }
    }
    return history
  }

  fun updateWith(upsert: NomisReport, clock: Clock) {
    val updatedBy = upsert.lastModifiedBy ?: upsert.createdBy
    val now = LocalDateTime.now(clock)

    this.modifiedIn = InformationSource.NOMIS

    this.type = Type.fromNomisCode(upsert.type)

    // NOTE: Currently we update the event information as well on update.
    //       For some of these fields makes more sense because that's explicitly
    //       the intent (e.g. `incidentDateTime`, `location`, etc... are also in the event)
    //       For Event's title/description may make less sense, but we're keeping this in
    //       as well for now.
    this.incidentDateAndTime = upsert.incidentDateTime
    this.event.eventDateAndTime = incidentDateAndTime

    this.location = upsert.prison.code
    this.event.location = location

    this.title = upsert.title ?: NO_DETAILS_GIVEN
    this.event.title = title

    this.description = upsert.description ?: NO_DETAILS_GIVEN
    this.event.description = description

    this.reportedBy = upsert.reportingStaff.username
    this.reportedAt = upsert.reportedDateTime

    val newStatus = Status.fromNomisCode(upsert.status.code)
    if (newStatus != status) {
      this.status = newStatus
      addStatusHistory(newStatus, now, updatedBy)
    }

    this.questionSetId = "${upsert.questionnaireId}"

    this.createdAt = upsert.createDateTime
    this.event.createdAt = upsert.createDateTime

    this.modifiedAt = upsert.lastModifiedDateTime ?: upsert.createDateTime
    this.event.modifiedAt = modifiedAt

    this.modifiedBy = updatedBy
    this.event.modifiedBy = updatedBy

    updateStaffInvolved(upsert.staffParties)
    updatePrisonerInvolved(upsert.offenderParties)
    updateCorrectionRequests(upsert.requirements)
    updateQuestionAndResponses(upsert.questions)
    updateHistory(upsert.history)
  }

  fun toDtoBasic() = ReportBasic(
    id = id!!,
    reportReference = reportReference,
    incidentDateAndTime = incidentDateAndTime,
    location = location,
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
    lastModifiedInNomis = modifiedIn == InformationSource.NOMIS,
  )

  fun toDtoWithDetails() = ReportWithDetails(
    id = id!!,
    reportReference = reportReference,
    incidentDateAndTime = incidentDateAndTime,
    location = location,
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
    lastModifiedInNomis = modifiedIn == InformationSource.NOMIS,
    event = event.toDto(),
    questions = questions.map { it.toDto() },
    history = history.map { it.toDto() },
    historyOfStatuses = historyOfStatuses.map { it.toDto() },
    staffInvolved = staffInvolved.map { it.toDto() },
    prisonersInvolved = prisonersInvolved.map { it.toDto() },
    correctionRequests = correctionRequests.map { it.toDto() },
    staffInvolvementDone = staffInvolvementDone,
    prisonerInvolvementDone = prisonerInvolvementDone,
  )
}
