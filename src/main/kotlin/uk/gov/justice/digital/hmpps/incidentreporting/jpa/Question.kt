package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.Hibernate
import org.hibernate.annotations.SortNatural
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisResponse
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SortedSet
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Question as QuestionDto

@Entity
@EntityOpen
class Question(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  /**
   * Identifier that must be unique within one report; used for updating questions in place.
   * Typically refers to a specific question for an incident type.
   */
  val code: String,

  val sequence: Int,

  /**
   * The question text as seen by downstream data consumers
   */
  var question: String,

  /**
   * Unused: could be a free-text response to a question
   */
  var additionalInformation: String? = null,

  @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val responses: SortedSet<Response> = sortedSetOf(),

) : Comparable<Question> {

  companion object {
    private val COMPARATOR = compareBy<Question>
      { it.report }
      .thenBy { it.sequence }
      .thenBy { it.code }
  }

  override fun compareTo(other: Question) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Question

    if (report != other.report) return false
    if (sequence != other.sequence) return false
    if (code != other.code) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + sequence.hashCode()
    result = 31 * result + code.hashCode()
    return result
  }

  override fun toString(): String {
    return "Question(id=$id, reportReference=${report.reportReference}, sequence=$sequence, code=$code)"
  }

  fun reset(
    question: String,
    additionalInformation: String? = null,
  ): Question {
    this.question = question
    this.additionalInformation = additionalInformation
    this.responses.clear()
    return this
  }

  fun updateResponses(nomisResponses: List<NomisResponse>) {
    this.responses.retainAll(
      nomisResponses.map { nomisResponse ->
        val newResponse = createResponse(nomisResponse, report.reportedAt)
        this.responses.find { it == newResponse }?.apply {
          response = newResponse.response
          responseDate = newResponse.responseDate
          additionalInformation = newResponse.additionalInformation
          recordedBy = newResponse.recordedBy
          recordedAt = newResponse.recordedAt
        } ?: addResponse(newResponse)
      }.toSet(),
    )
  }

  fun createResponse(nomisResponse: NomisResponse, recordedAt: LocalDateTime): Response =
    Response(
      question = this,
      response = nomisResponse.answer!!,
      sequence = nomisResponse.sequence,
      responseDate = nomisResponse.responseDate,
      additionalInformation = nomisResponse.comment,
      recordedBy = nomisResponse.recordingStaff.username,
      recordedAt = recordedAt,
    )

  fun addResponse(response: Response): Response {
    this.responses.add(response)
    return response
  }

  fun addResponse(
    response: String,
    responseDate: LocalDate? = null,
    sequence: Int,
    additionalInformation: String? = null,
    recordedBy: String,
    recordedAt: LocalDateTime,
  ): Question {
    addResponse(
      Response(
        question = this,
        response = response,
        sequence = sequence,
        responseDate = responseDate,
        additionalInformation = additionalInformation,
        recordedBy = recordedBy,
        recordedAt = recordedAt,
      ),
    )
    return this
  }

  fun toDto() = QuestionDto(
    code = code,
    question = question,
    sequence = sequence,
    additionalInformation = additionalInformation,
    responses = responses.map { it.toDto() },
  )
}
