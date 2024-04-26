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
import jakarta.persistence.OrderColumn
import org.hibernate.Hibernate
import org.hibernate.annotations.GenericGenerator
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.io.Serializable
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report as ReportDTO

@Entity
class Report(
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", updatable = false, nullable = false)
  val id: UUID? = null,

  /**
   * Human readable ID.
   * A number when sourced from NOMIS.
   * Prefixed with “IR-” when sourced from DPS.
   */
  @Column(nullable = false, unique = true, length = 25)
  val incidentNumber: String,

  val incidentDateAndTime: LocalDateTime,

  val prisonId: String,

  @Enumerated(EnumType.STRING)
  private var type: Type,

  var title: String,
  var description: String,

  val reportedBy: String,
  val reportedDate: LocalDateTime,
  @Enumerated(EnumType.STRING)
  var status: Status = Status.DRAFT,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], optional = false)
  val event: Event,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val historyOfStatuses: MutableList<StatusHistory> = mutableListOf(),

  val assignedTo: String,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val staffInvolved: MutableList<StaffInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val prisonersInvolved: MutableList<PrisonerInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val locations: MutableList<Location> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val evidence: MutableList<Evidence> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val correctionRequests: MutableList<CorrectionRequest> = mutableListOf(),

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence", nullable = false)
  private val questions: MutableList<Question> = mutableListOf(),

  val questionSetId: String? = null,

  @OneToMany(mappedBy = "report", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val history: MutableList<History> = mutableListOf(),

  @Enumerated(EnumType.STRING)
  val source: InformationSource = InformationSource.DPS,

  val createdDate: LocalDateTime,
  var lastModifiedDate: LocalDateTime,
  var lastModifiedBy: String,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Report

    return incidentNumber == other.incidentNumber
  }

  override fun hashCode(): Int {
    return incidentNumber.hashCode()
  }

  fun getQuestions(): List<Question> = questions

  fun getType() = type

  fun changeType(newType: Type, changedDate: LocalDateTime, staffChanged: String) {
    copyToHistory(changedDate, staffChanged)
    questions.clear()
    type = newType
  }

  fun addEvidence(type: String, description: String): Evidence {
    val evidenceItem =
      Evidence(report = this, type = type, description = description)
    evidence.add(evidenceItem)
    return evidenceItem
  }

  fun addStaffInvolved(staffRole: StaffRole, username: String, comment: String? = null): StaffInvolvement {
    val staff = StaffInvolvement(report = this, staffUsername = username, staffRole = staffRole, comment = comment)
    staffInvolved.add(staff)
    return staff
  }

  fun addPrisonerInvolved(
    prisonerNumber: String,
    prisonerInvolvement: PrisonerRole,
    prisonerOutcome: PrisonerOutcome? = null,
    comment: String? = null,
  ): PrisonerInvolvement {
    val prisoner = PrisonerInvolvement(
      report = this,
      prisonerNumber = prisonerNumber,
      prisonerInvolvement = prisonerInvolvement,
      outcome = prisonerOutcome,
      comment = comment,
    )
    prisonersInvolved.add(prisoner)
    return prisoner
  }

  fun addLocation(
    locationId: String,
    locationType: String,
    description: String,
  ): Location {
    val location = Location(
      report = this,
      locationId = locationId,
      type = locationType,
      description = description,
    )
    locations.add(location)
    return location
  }

  fun addCorrectionRequest(
    correctionRequestedBy: String,
    correctionRequestedAt: LocalDateTime,
    reason: CorrectionReason,
    descriptionOfChange: String,
  ): CorrectionRequest {
    val correctionRequest = CorrectionRequest(
      report = this,
      correctionRequestedBy = correctionRequestedBy,
      correctionRequestedAt = correctionRequestedAt,
      reason = reason,
      descriptionOfChange = descriptionOfChange,
    )
    correctionRequests.add(correctionRequest)
    return correctionRequest
  }

  fun addQuestion(
    code: String,
    question: String? = null,
  ): Question {
    val questionItem = Question(
      report = this,
      code = code,
      question = question,
    )
    questions.add(questionItem)
    return questionItem
  }

  fun addHistory(type: Type, incidentChangeDate: LocalDateTime, staffChanged: String): History {
    val historyItem = History(
      report = this,
      type = type,
      changeDate = incidentChangeDate,
      changeStaffUsername = staffChanged,
    )
    history.add(historyItem)
    return historyItem
  }

  fun copyToHistory(changedDate: LocalDateTime, staffChanged: String): History {
    val history = addHistory(type, changedDate, staffChanged)
    getQuestions().filterNotNull().forEach { question ->
      val historicalQuestion = history.addQuestion(question.code, question.question)
      question.getEvidence()?.let { historicalQuestion.attachEvidence(it) }
      question.getLocation()?.let { historicalQuestion.attachLocation(it) }
      question.getStaffInvolvement()?.let { historicalQuestion.attachStaffInvolvement(it) }
      question.getPrisonerInvolvement()?.let { historicalQuestion.attachPrisonerInvolvement(it) }
    }
    return history
  }

  fun updateWith(upsert: NomisIncidentReport, updatedBy: String, clock: Clock) {
    this.title = upsert.title ?: "NO DETAILS GIVEN"
    this.description = upsert.description ?: "NO DETAILS GIVEN"
    this.status = mapIncidentStatus(upsert.status.code)
    this.lastModifiedBy = updatedBy
    this.lastModifiedDate = LocalDateTime.now(clock)
  }

  fun toDto(): ReportDTO =
    ReportDTO(
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
    )
}
