package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.validation.ValidationException
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
class HistoricalIncidentResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incidentHistory: IncidentHistory,

  override val dataItem: String,

  override val dataItemDescription: String? = null,

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence")
  @JoinColumn(name = "incident_response_id", nullable = false)
  private val responses: MutableList<HistoricalResponse> = mutableListOf(),

  @OneToOne(fetch = FetchType.LAZY)
  private var evidence: Evidence? = null,

  @OneToOne(fetch = FetchType.LAZY)
  private var location: IncidentLocation? = null,

  @OneToOne(fetch = FetchType.LAZY)
  private var prisonerInvolvement: PrisonerInvolvement? = null,

  @OneToOne(fetch = FetchType.LAZY)
  private var staffInvolvement: StaffInvolvement? = null,

) : GenericQuestion {

  override fun addAnswer(itemValue: String, additionalInformation: String?, recordedBy: String, recordedOn: LocalDateTime): HistoricalIncidentResponse {
    responses.add(
      HistoricalResponse(
        itemValue = itemValue,
        recordedBy = recordedBy,
        recordedOn = recordedOn,
        additionalInformation = additionalInformation,
      ),
    )
    return this
  }

  override fun getLocation() = location

  override fun attachLocation(location: IncidentLocation) {
    if (location.getIncident() != incidentHistory.getIncident()) {
      throw ValidationException("Cannot attach a location from a different incident report")
    }
    this.location = location
  }

  override fun getPrisonerInvolvement() = prisonerInvolvement

  override fun attachPrisonerInvolvement(prisonerInvolvement: PrisonerInvolvement) {
    if (prisonerInvolvement.getIncident() != incidentHistory.getIncident()) {
      throw ValidationException("Cannot attach prisoner involvement from a different incident report")
    }
    this.prisonerInvolvement = prisonerInvolvement
  }

  override fun getStaffInvolvement() = staffInvolvement

  override fun attachStaffInvolvement(staffInvolvement: StaffInvolvement) {
    if (staffInvolvement.getIncident() != incidentHistory.getIncident()) {
      throw ValidationException("Cannot attach staff involvement from a different incident report")
    }
    this.staffInvolvement = staffInvolvement
  }

  override fun getEvidence() = evidence

  override fun attachEvidence(evidence: Evidence) {
    if (evidence.getIncident() != incidentHistory.getIncident()) {
      throw ValidationException("Cannot attach evidence from a different incident report")
    }
    this.evidence = evidence
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as HistoricalIncidentResponse

    if (incidentHistory != other.incidentHistory) return false
    if (dataItem != other.dataItem) return false

    return true
  }

  override fun hashCode(): Int {
    var result = incidentHistory.hashCode()
    result = 31 * result + dataItem.hashCode()
    return result
  }

  override fun toString(): String {
    return "HistoricalIncidentResponse(dataItem='$dataItem', responses=$responses)"
  }
}
