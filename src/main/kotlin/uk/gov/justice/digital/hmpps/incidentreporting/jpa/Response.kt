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

  /**
   * Identifier that refers to a specific answer for an incident type.
   */
  val code: String,

  val sequence: Int,

  /**
   * The response text as seen by downstream data consumers
   */
  var response: String,

  /**
   * The response text as seen by the users at the point of entry
   */
  var label: String,

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

  companion object {
    private val COMPARATOR = compareBy<Response>
      { it.question }
      .thenBy { it.code }
  }

  override fun compareTo(other: Response) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Response

    if (question != other.question) return false
    if (code != other.code) return false

    return true
  }

  override fun hashCode(): Int {
    var result = question.hashCode()
    result = 31 * result + code.hashCode()
    return result
  }

  override fun toString(): String {
    return "Response(id=$id, questionId=${question.id}, code=$code, code=$code, response=$response)"
  }

  fun toDto() = ResponseDto(
    code = code,
    response = response,
    label = label,
    sequence = sequence,
    responseDate = responseDate,
    recordedBy = recordedBy,
    recordedAt = recordedAt,
    additionalInformation = additionalInformation,
  )
}
