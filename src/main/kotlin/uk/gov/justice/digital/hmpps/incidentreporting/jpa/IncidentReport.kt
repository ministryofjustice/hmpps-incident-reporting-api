package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.Hibernate
import org.hibernate.annotations.GenericGenerator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

@Entity
class IncidentReport(
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  @Column(name = "id", updatable = false, nullable = false)
  val id: UUID? = null,

  @Column(nullable = false, unique = true, length = 10)
  val incidentNumber: String,

  val incidentDateAndTime: LocalDateTime,

  val prisonId: String,

  @Enumerated(EnumType.STRING)
  val incidentType: IncidentType,

  val incidentDetails: String,

  val reportedBy: String,
  val reportedDate: LocalDateTime,
  @Enumerated(EnumType.STRING)
  val status: IncidentStatus = IncidentStatus.DRAFT,

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
  val incidentResponses: MutableList<IncidentResponse> = mutableListOf(),

  @OneToMany(mappedBy = "incident", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val historyOfResponses: MutableList<HistoricalIncidentResponse> = mutableListOf(),

  val createdDate: LocalDateTime,
  val lastModifiedDate: LocalDateTime,
  val lastModifiedBy: String,

) : Serializable {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as IncidentReport

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  fun addEvidence(typeOfEvidence: String, evidenceDescription: String): Evidence {
    val evidenceItem = Evidence(incident = this, typeOfEvidence = typeOfEvidence, descriptionOfEvidence = evidenceDescription)
    evidence.add(evidenceItem)
    return evidenceItem
  }

  fun addStaffInvolved(staffRole: StaffRole, username: String): StaffInvolvement {
    val staff = StaffInvolvement(incident = this, staffUsername = username, staffRole = staffRole)
    staffInvolved.add(staff)
    return staff
  }

  fun addPrisonerInvolved(prisonerNumber: String, prisonerInvolvement: PrisonerRole): PrisonerInvolvement {
    val prisoner = PrisonerInvolvement(incident = this, prisonerNumber = prisonerNumber, prisonerInvolvement = prisonerInvolvement)
    prisonersInvolved.add(prisoner)
    return prisoner
  }

  fun addOtherPersonInvolved(personName: String, otherPersonType: PersonRole): OtherPersonInvolvement {
    val otherPersonInvolved = OtherPersonInvolvement(incident = this, personName = personName, personType = otherPersonType)
    otherPeopleInvolved.add(otherPersonInvolved)
    return otherPersonInvolved
  }

  fun addIncidentLocation(locationId: String, locationType: String, locationDescription: String? = null): IncidentLocation {
    val incidentLocation = IncidentLocation(incident = this, locationId = locationId, locationType = locationType, locationDescription = locationDescription)
    locations.add(incidentLocation)
    return incidentLocation
  }

  fun addDataPoint(key: String, value: String, recordedBy: String, recordedOn: LocalDateTime, comment: String? = null, moreInfo: String? = null, evidence: Evidence? = null, location: IncidentLocation? = null, otherPersonInvolvement: OtherPersonInvolvement? = null, prisonerInvolvement: PrisonerInvolvement? = null, staffInvolvement: StaffInvolvement? = null): IncidentResponse {
    val incidentResponse = IncidentResponse(incident = this, dataPointKey = key, comment = comment, evidence = evidence, location = location, otherPersonInvolvement = otherPersonInvolvement, prisonerInvolvement = prisonerInvolvement, staffInvolvement = staffInvolvement, recordedBy = recordedBy, recordedOn = recordedOn)
    incidentResponse.addDataPointValue(value, moreInfo)
    incidentResponses.add(incidentResponse)
    return incidentResponse
  }
}
