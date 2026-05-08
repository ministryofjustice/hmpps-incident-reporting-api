package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedEntityGraphs
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import org.hibernate.Hibernate
import org.hibernate.annotations.SortNatural
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserType
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.id.GeneratedUuidV7
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ObjectAtIndexNotFoundException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.QuestionsNotFoundException
import java.time.LocalDateTime
import java.util.SortedSet
import java.util.UUID

@Entity
@NamedEntityGraphs(
  value = [
    NamedEntityGraph(
      name = "Report.eager",
      attributeNodes = [
        NamedAttributeNode("descriptionAddendums"),
        NamedAttributeNode("staffInvolved"),
        NamedAttributeNode("prisonersInvolved"),
        NamedAttributeNode("correctionRequests"),
        NamedAttributeNode("history"),
        NamedAttributeNode("questions", subgraph = "questions.eager.subgraph"),
      ],
      subgraphs = [
        NamedSubgraph(
          name = "questions.eager.subgraph",
          attributeNodes = [
            NamedAttributeNode("responses"),
          ],
        ),
      ],
    ),
    NamedEntityGraph(
      name = "Report.eager.history",
      attributeNodes = [
        NamedAttributeNode("descriptionAddendums"),
        NamedAttributeNode("staffInvolved"),
        NamedAttributeNode("prisonersInvolved"),
        NamedAttributeNode("correctionRequests"),
        NamedAttributeNode("history", subgraph = "history.eager.subgraph"),
        NamedAttributeNode("historyOfStatuses"),
        NamedAttributeNode("questions", subgraph = "questions.eager.subgraph"),
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
   * Internal ID which users should not see
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
  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val descriptionAddendums: SortedSet<DescriptionAddendum> = sortedSetOf(),

  var reportedBy: String,
  var reportedAt: LocalDateTime,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val historyOfStatuses: SortedSet<StatusHistory> = sortedSetOf(),

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

  @Enumerated(EnumType.STRING)
  @Column(name = "last_user_action", nullable = true)
  var lastUserAction: UserAction? = null,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val questions: SortedSet<Question> = sortedSetOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val history: SortedSet<History> = sortedSetOf(),

  var createdAt: LocalDateTime,
  var modifiedAt: LocalDateTime,
  var modifiedBy: String,
  var duplicatedReportId: UUID? = null,
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
    if (type != newType) {
      // archive existing questions and responses
      copyToHistory(changedAt, changedBy)
      // remove all questions and responses since new type will have a different set
      questions.clear()
      type = newType
      // status is not changed, client applications should decide the new status
      // remove all prisoner involvements because roles may not be allowed in new type
      prisonersInvolved.clear()
      // keep staff involvements because all roles are available in all types
    }
    return this
  }

  fun changeStatus(
    newStatus: Status,
    changedAt: LocalDateTime,
    changedBy: String,
  ): Report {
    if (status != newStatus) {
      status = newStatus
      addStatusHistory(newStatus, changedAt, changedBy)
    }
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

  fun findDescriptionAddendumByIndex(index: Int): DescriptionAddendum = descriptionAddendums.elementAtOrNull(index - 1)
    ?: throw ObjectAtIndexNotFoundException(StaffInvolvement::class, index)

  fun addDescriptionAddendum(
    sequence: Int,
    createdBy: String,
    createdAt: LocalDateTime,
    firstName: String,
    lastName: String,
    text: String,
  ): DescriptionAddendum {
    return DescriptionAddendum(
      report = this,
      sequence = sequence,
      createdBy = createdBy,
      firstName = firstName,
      lastName = lastName,
      createdAt = createdAt,
      text = text,
    ).also { descriptionAddendums.add(it) }
  }

  fun removeDescriptionAddendum(addendum: DescriptionAddendum) {
    descriptionAddendums.remove(addendum)
  }

  fun findStaffInvolvedByIndex(index: Int): StaffInvolvement = staffInvolved.elementAtOrNull(index - 1)
    ?: throw ObjectAtIndexNotFoundException(StaffInvolvement::class, index)

  private fun addStaffInvolved(staffInvolvement: StaffInvolvement): StaffInvolvement {
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

  private fun addPrisonerInvolved(prisonerInvolvement: PrisonerInvolvement): PrisonerInvolvement {
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

  private fun addCorrectionRequest(correctionRequest: CorrectionRequest): CorrectionRequest {
    this.correctionRequests.add(correctionRequest)
    // Update last user action to reflect the most recent correction request added (maybe null)
    this.lastUserAction = correctionRequest.userAction
    return correctionRequest
  }

  fun addCorrectionRequest(
    sequence: Int,
    correctionRequestedBy: String,
    correctionRequestedAt: LocalDateTime,
    descriptionOfChange: String,
    location: String? = null,
    userAction: UserAction? = null,
    originalReportReference: String? = null,
    userType: UserType? = null,
  ): CorrectionRequest {
    return addCorrectionRequest(
      CorrectionRequest(
        report = this,
        sequence = sequence,
        correctionRequestedBy = correctionRequestedBy,
        correctionRequestedAt = correctionRequestedAt,
        descriptionOfChange = descriptionOfChange,
        location = location,
        userAction = userAction,
        originalReportReference = originalReportReference,
        userType = userType,
      ),
    )
  }

  fun removeCorrectionRequest(correctionRequest: CorrectionRequest) {
    correctionRequests.remove(correctionRequest)
  }

  fun addQuestion(
    code: String,
    question: String,
    label: String,
    sequence: Int,
    additionalInformation: String? = null,
  ): Question {
    return Question(
      report = this,
      code = code,
      question = question,
      label = label,
      sequence = sequence,
      additionalInformation = additionalInformation,
    ).also { questions.add(it) }
  }

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

  private fun copyToHistory(changedAt: LocalDateTime, changedBy: String): History {
    val history = addHistory(type, changedAt, changedBy)
    questions.forEach { question ->
      val historicalQuestion = history.addQuestion(
        code = question.code,
        question = question.question,
        label = question.label,
        sequence = question.sequence,
        additionalInformation = question.additionalInformation,
      )
      question.responses.forEach { response ->
        historicalQuestion.addResponse(
          code = response.code,
          response = response.response,
          label = response.label,
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
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    createdInNomis = source == InformationSource.NOMIS,
    lastModifiedInNomis = modifiedIn == InformationSource.NOMIS,
    duplicatedReportId = duplicatedReportId,
    latestUserAction = lastUserAction,
  )

  fun toDtoWithDetails(includeHistory: Boolean = false) = ReportWithDetails(
    id = id!!,
    reportReference = reportReference,
    incidentDateAndTime = incidentDateAndTime,
    location = location,
    type = type,
    title = title,
    description = description,
    descriptionAddendums = descriptionAddendums.map { it.toDto() },
    reportedBy = reportedBy,
    reportedAt = reportedAt,
    status = status,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    createdInNomis = source == InformationSource.NOMIS,
    lastModifiedInNomis = modifiedIn == InformationSource.NOMIS,
    duplicatedReportId = duplicatedReportId,
    questions = questions.map { it.toDto() },
    history = if (includeHistory) {
      history.map { it.toDto() }
    } else {
      emptyList()
    },
    historyOfStatuses = historyOfStatuses.map { it.toDto() },
    incidentTypeHistory = history.map { it.toIncidentTypeHistoryDto() },
    staffInvolved = staffInvolved.map { it.toDto() },
    prisonersInvolved = prisonersInvolved.map { it.toDto() },
    correctionRequests = correctionRequests.map { it.toDto() },
    staffInvolvementDone = staffInvolvementDone,
    prisonerInvolvementDone = prisonerInvolvementDone,
    latestUserAction = lastUserAction,
  )
}
