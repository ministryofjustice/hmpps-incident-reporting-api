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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.NomisIncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.io.Serializable
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport as IncidentReportDTO

@Entity
class IncidentReport(
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", updatable = false, nullable = false)
  val id: UUID? = null,

  @Column(nullable = false, unique = true, length = 25)
  val incidentNumber: String,

  val incidentDateAndTime: LocalDateTime,

  val prisonId: String,

  @Enumerated(EnumType.STRING)
  val incidentType: IncidentType,

  var summary: String?,
  var incidentDetails: String,

  val reportedBy: String,
  val reportedDate: LocalDateTime,
  @Enumerated(EnumType.STRING)
  var status: IncidentStatus = IncidentStatus.DRAFT,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], optional = false)
  val event: IncidentEvent,

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val historyOfStatuses: MutableList<StatusHistory> = mutableListOf(),

  val assignedTo: String,

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val staffInvolved: MutableList<StaffInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val prisonersInvolved: MutableList<PrisonerInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val otherPeopleInvolved: MutableList<OtherPersonInvolvement> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val locations: MutableList<IncidentLocation> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val evidence: MutableList<Evidence> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val incidentCorrectionRequests: MutableList<IncidentCorrectionRequest> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @OrderColumn(name = "sequence")
  val incidentResponses: MutableList<IncidentResponse> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val history: MutableList<IncidentHistory> = mutableListOf(),

  @Enumerated(EnumType.STRING)
  val source: InformationSource = InformationSource.DPS,

  val questionSetId: String? = null,

  val createdDate: LocalDateTime,
  var lastModifiedDate: LocalDateTime,
  var lastModifiedBy: String,

) : Serializable {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as IncidentReport

    return incidentNumber == other.incidentNumber
  }

  override fun hashCode(): Int {
    return incidentNumber.hashCode()
  }

  fun addEvidence(typeOfEvidence: String, evidenceDescription: String): Evidence {
    val evidenceItem =
      Evidence(incident = this, typeOfEvidence = typeOfEvidence, descriptionOfEvidence = evidenceDescription)
    evidence.add(evidenceItem)
    return evidenceItem
  }

  fun addStaffInvolved(staffRole: StaffRole, username: String, comment: String? = null): StaffInvolvement {
    val staff = StaffInvolvement(incident = this, staffUsername = username, staffRole = staffRole, comment = comment)
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
      incident = this,
      prisonerNumber = prisonerNumber,
      prisonerInvolvement = prisonerInvolvement,
      outcome = prisonerOutcome,
      comment = comment,
    )
    prisonersInvolved.add(prisoner)
    return prisoner
  }

  fun addOtherPersonInvolved(personName: String, otherPersonType: PersonRole): OtherPersonInvolvement {
    val otherPersonInvolved =
      OtherPersonInvolvement(incident = this, personName = personName, personType = otherPersonType)
    otherPeopleInvolved.add(otherPersonInvolved)
    return otherPersonInvolved
  }

  fun addIncidentLocation(
    locationId: String,
    locationType: String,
    locationDescription: String? = null,
  ): IncidentLocation {
    val incidentLocation = IncidentLocation(
      incident = this,
      locationId = locationId,
      locationType = locationType,
      locationDescription = locationDescription,
    )
    locations.add(incidentLocation)
    return incidentLocation
  }

  fun addCorrectionRequest(correctionRequestedBy: String, correctionRequestedAt: LocalDateTime, reason: CorrectionReason, descriptionOfChange: String?) {
    val correctionRequest = IncidentCorrectionRequest(
      incident = this,
      correctionRequestedBy = correctionRequestedBy,
      correctionRequestedAt = correctionRequestedAt,
      reason = reason,
      descriptionOfChange = descriptionOfChange,
    )
    incidentCorrectionRequests.add(correctionRequest)
  }

  fun addIncidentData(
    dataItem: String,
    dataItemDescription: String? = null,
  ): IncidentResponse {
    val incidentResponse = IncidentResponse(
      incident = this,
      dataItem = dataItem,
      dataItemDescription = dataItemDescription,
    )
    incidentResponses.add(incidentResponse)
    return incidentResponse
  }

  fun addIncidentHistory(incidentType: IncidentType, incidentChangeDate: LocalDateTime, staffChanged: String): IncidentHistory {
    val incidentHistory = IncidentHistory(
      incident = this,
      incidentType = incidentType,
      incidentChangeDate = incidentChangeDate,
      incidentChangeStaffUsername = staffChanged,
    )

    history.add(incidentHistory)
    return incidentHistory
  }

  fun updateWith(upsert: NomisIncidentReport, updatedBy: String, clock: Clock) {
    this.incidentDetails = upsert.description ?: "NO DETAILS GIVEN"
    this.status = mapIncidentStatus(upsert.status.code)
    this.summary = upsert.title
    this.lastModifiedBy = updatedBy
    this.lastModifiedDate = LocalDateTime.now(clock)
  }

  fun toDto(): IncidentReportDTO =
    IncidentReportDTO(
      id = this.id!!,
      incidentNumber = this.incidentNumber,
      incidentDateAndTime = this.incidentDateAndTime,
      prisonId = this.prisonId,
      incidentType = this.incidentType,
      summary = this.summary,
      incidentDetails = this.incidentDetails,
      reportedBy = this.reportedBy,
      reportedDate = this.reportedDate,
      status = this.status,
      assignedTo = this.assignedTo,
      createdDate = this.createdDate,
      lastModifiedDate = this.lastModifiedDate,
      lastModifiedBy = this.lastModifiedBy,
      createdInNomis = this.source == InformationSource.NOMIS,
      event = this.event.toDto(),
    )
}
