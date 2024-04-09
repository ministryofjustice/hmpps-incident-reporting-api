package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
class IncidentHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incident: IncidentReport,

  @Enumerated(EnumType.STRING)
  val incidentType: IncidentType,

  val incidentChangeDate: LocalDateTime,

  val incidentChangeStaffUsername: String,

  @OneToMany(mappedBy = "incidentHistory", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val historyOfResponses: MutableList<HistoricalIncidentResponse> = mutableListOf(),
) {

  fun addHistoricalResponse(
    dataItem: String,
    dataItemDescription: String? = null,
  ): HistoricalIncidentResponse {
    val historicalResponse = HistoricalIncidentResponse(
      incidentHistory = this,
      dataItem = dataItem,
      dataItemDescription = dataItemDescription,
    )
    historyOfResponses.add(historicalResponse)
    return historicalResponse
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as IncidentHistory

    if (incident != other.incident) return false
    if (incidentType != other.incidentType) return false
    if (incidentChangeDate != other.incidentChangeDate) return false

    return true
  }

  override fun hashCode(): Int {
    var result = incident.hashCode()
    result = 31 * result + incidentType.hashCode()
    result = 31 * result + incidentChangeDate.hashCode()
    return result
  }
}
