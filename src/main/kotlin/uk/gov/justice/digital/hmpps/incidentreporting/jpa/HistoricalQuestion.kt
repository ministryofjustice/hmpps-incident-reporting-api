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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistoryResponse
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SortedSet
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalQuestion as HistoricalQuestionDto

@Entity
@EntityOpen
class HistoricalQuestion(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val history: History,

  /**
   * Identifier that must be unique within one History item in a report; used for updating questions in place.
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
  val additionalInformation: String? = null,

  @OneToMany(mappedBy = "historicalQuestion", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val responses: SortedSet<HistoricalResponse> = sortedSetOf(),
) : Comparable<HistoricalQuestion> {

  override fun toString(): String {
    return "HistoricalQuestion (history=${history.id}, code = $code, seq = $sequence)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as HistoricalQuestion

    if (history != other.history) return false
    if (sequence != other.sequence) return false
    if (code != other.code) return false

    return true
  }

  override fun hashCode(): Int {
    var result = history.hashCode()
    result = 31 * result + sequence.hashCode()
    result = 31 * result + code.hashCode()
    return result
  }

  companion object {
    private val COMPARATOR = compareBy<HistoricalQuestion>
      { it.history }
      .thenBy { it.sequence }
      .thenBy { it.code }
  }

  override fun compareTo(other: HistoricalQuestion) = COMPARATOR.compare(this, other)

  fun updateResponses(nomisResponses: List<NomisHistoryResponse>, reportedAt: LocalDateTime) {
    this.responses.retainAll(
      nomisResponses.map { nomisResponse ->
        val newResponse = createHistoricalResponse(nomisResponse, reportedAt)
        this.responses.find { it == newResponse }?.apply {
          responseDate = newResponse.responseDate
          additionalInformation = newResponse.additionalInformation
          recordedBy = newResponse.recordedBy
          recordedAt = newResponse.recordedAt
        } ?: addResponse(newResponse)
      }.toSet(),
    )
  }

  fun createHistoricalResponse(
    answer: NomisHistoryResponse,
    recordedAt: LocalDateTime,
  ) =
    HistoricalResponse(
      historicalQuestion = this,
      response = answer.answer!!,
      sequence = answer.responseSequence - 1,
      responseDate = answer.responseDate,
      additionalInformation = answer.comment,
      recordedBy = answer.recordingStaff.username,
      recordedAt = recordedAt,
    )

  fun addResponse(response: HistoricalResponse): HistoricalResponse {
    this.responses.add(response)
    return response
  }

  fun addResponse(
    response: String,
    sequence: Int,
    responseDate: LocalDate? = null,
    additionalInformation: String? = null,
    recordedBy: String,
    recordedAt: LocalDateTime,
  ): HistoricalQuestion {
    addResponse(
      HistoricalResponse(
        historicalQuestion = this,
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

  fun toDto() = HistoricalQuestionDto(
    code = code,
    question = question,
    sequence = sequence,
    additionalInformation = additionalInformation,
    responses = responses.map { it.toDto() },
  )
}
