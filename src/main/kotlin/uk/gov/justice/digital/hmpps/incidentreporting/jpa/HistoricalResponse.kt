package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalResponse as HistoricalResponseDto

@Entity
class HistoricalResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val historicalQuestion: HistoricalQuestion,

  // TODO: should we add a `val code: String` like in Question?
  val response: String,

  val additionalInformation: String? = null,

  val recordedBy: String,
  val recordedOn: LocalDateTime,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as HistoricalResponse

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "HistoricalResponse(id=$id)"
  }

  fun toDto() = HistoricalResponseDto(
    response = response,
    recordedBy = recordedBy,
    recordedOn = recordedOn,
    additionalInformation = additionalInformation,
  )
}
