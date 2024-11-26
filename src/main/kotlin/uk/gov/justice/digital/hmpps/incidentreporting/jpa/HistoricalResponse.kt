package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalResponse as HistoricalResponseDto

@Entity
class HistoricalResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val historicalQuestion: HistoricalQuestion,

  // TODO: should we add a `val code: String` like in HistoricalQuestion?

  /**
   * The response text as seen by downstream data consumers
   */
  val response: String,

  /**
   * Optional date attached to response
   */
  val responseDate: LocalDate? = null,

  /**
   * Optional comment attached to response
   */
  val additionalInformation: String? = null,

  val recordedBy: String,
  val recordedAt: LocalDateTime,
) {
  override fun toString(): String {
    return "HistoricalResponse(id=$id)"
  }

  fun toDto() = HistoricalResponseDto(
    response = response,
    responseDate = responseDate,
    recordedBy = recordedBy,
    recordedAt = recordedAt,
    additionalInformation = additionalInformation,
  )
}
