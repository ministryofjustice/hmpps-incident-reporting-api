package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalResponse as HistoricalResponseDto

@Entity
@EntityOpen
class HistoricalResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val historicalQuestion: HistoricalQuestion,

  val sequence: Int,

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
) : Comparable<HistoricalResponse> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as HistoricalResponse

    if (historicalQuestion != other.historicalQuestion) return false
    if (sequence != other.sequence) return false

    return true
  }

  override fun hashCode(): Int {
    var result = historicalQuestion.hashCode()
    result = 31 * result + sequence.hashCode()
    return result
  }

  companion object {
    private val COMPARATOR = compareBy<HistoricalResponse>
      { it.historicalQuestion.id }
      .thenBy { it.sequence }
  }

  override fun compareTo(other: HistoricalResponse) = COMPARATOR.compare(this, other)

  fun toDto() = HistoricalResponseDto(
    response = response,
    sequence = sequence,
    responseDate = responseDate,
    recordedBy = recordedBy,
    recordedAt = recordedAt,
    additionalInformation = additionalInformation,
  )

  override fun toString(): String {
    return "HistoricalResponse(historicalQuestion=$historicalQuestion, sequence=$sequence, response='$response')"
  }
}
