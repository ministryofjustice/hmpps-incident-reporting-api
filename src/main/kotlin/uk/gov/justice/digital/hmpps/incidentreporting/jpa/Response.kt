package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
class Response(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incidentResponse: IncidentResponse,

  val itemValue: String,

  val recordedBy: String,

  val recordedOn: LocalDateTime,

  val additionalInformation: String? = null,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Response

    if (incidentResponse != other.incidentResponse) return false
    if (itemValue != other.itemValue) return false

    return true
  }

  override fun hashCode(): Int {
    var result = incidentResponse.hashCode()
    result = 31 * result + itemValue.hashCode()
    return result
  }

  override fun toString(): String {
    return "$itemValue recorded by $recordedBy on $recordedOn with additional info: $additionalInformation"
  }
}
