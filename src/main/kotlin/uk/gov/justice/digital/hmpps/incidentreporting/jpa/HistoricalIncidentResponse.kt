package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
class HistoricalIncidentResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incidentHistory: IncidentHistory,

  val dataItem: String,

  val dataItemDescription: String? = null,

  @OneToMany(mappedBy = "incidentResponse", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @OrderColumn(name = "sequence")
  val responses: MutableList<HistoricalResponse> = mutableListOf(),

  @OneToOne(fetch = FetchType.LAZY)
  val evidence: Evidence? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val location: IncidentLocation? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val prisonerInvolvement: PrisonerInvolvement? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val staffInvolvement: StaffInvolvement? = null,

) {
  fun addDataItem(itemValue: String, additionalInformation: String? = null, recordedBy: String, recordedOn: LocalDateTime): HistoricalIncidentResponse {
    responses.add(
      HistoricalResponse(
        incidentResponse = this,
        itemValue = itemValue,
        recordedBy = recordedBy,
        recordedOn = recordedOn,
        additionalInformation = additionalInformation,
      ),
    )
    return this
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
