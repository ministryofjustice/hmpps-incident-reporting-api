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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Response as ResponseDto

@Entity
@EntityOpen
class Response(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val question: Question,

  val sequence: Int,

  /**
   * The response text as seen by downstream data consumers
   */
  val response: String,

  /**
   * Optional date attached to response
   */
  var responseDate: LocalDate? = null,

  /**
   * Optional comment attached to response
   */
  var additionalInformation: String? = null,

  var recordedBy: String,
  var recordedAt: LocalDateTime,
) : Comparable<Response> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Response

    if (question != other.question) return false
    if (sequence != other.sequence) return false
    if (response != other.response) return false

    return true
  }

  override fun hashCode(): Int {
    var result = question.hashCode()
    result = 31 * result + sequence.hashCode()
    result = 31 * result + response.hashCode()

    return result
  }

  companion object {
    private val COMPARATOR = compareBy<Response>
      { it.question }
      .thenBy { it.sequence }
      .thenBy { it.response }
  }

  override fun compareTo(other: Response) = COMPARATOR.compare(this, other)

  fun toDto() = ResponseDto(
    response = response,
    sequence = sequence,
    responseDate = responseDate,
    recordedBy = recordedBy,
    recordedAt = recordedAt,
    additionalInformation = additionalInformation,
  )

  override fun toString(): String {
    return "Response(question=$question, sequence=$sequence, response='$response')"
  }
}
