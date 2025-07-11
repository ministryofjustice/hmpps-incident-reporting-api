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
   * The question text as seen by the users at the point of entry
   */
  var label: String,

  /**
   * Unused: could be a free-text response to a question
   */
  val additionalInformation: String? = null,

  @OneToMany(mappedBy = "historicalQuestion", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val responses: SortedSet<HistoricalResponse> = sortedSetOf(),
) : Comparable<HistoricalQuestion> {

  companion object {
    private val COMPARATOR = compareBy<HistoricalQuestion>
      { it.history }
      .thenBy { it.sequence }
      .thenBy { it.code }
  }

  override fun compareTo(other: HistoricalQuestion) = COMPARATOR.compare(this, other)

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

  override fun toString(): String {
    return "HistoricalQuestion(id=$id, historyId=${history.id}, sequence=$sequence, code=$code)"
  }

  fun updateResponses(nomisResponses: List<NomisHistoryResponse>, reportedAt: LocalDateTime) {
    this.responses.retainAll(
      nomisResponses.map { nomisResponse ->
        val newResponse = createResponse(nomisResponse, reportedAt)
        newResponse?.let { newVersion ->
          this.responses.find { it == newVersion }?.apply {
            response = newVersion.response
            responseDate = newVersion.responseDate
            additionalInformation = newVersion.additionalInformation
            recordedBy = newVersion.recordedBy
            recordedAt = newVersion.recordedAt
          } ?: addResponse(newVersion)
        }
      }.toSet(),
    )
  }

  fun createResponse(nomisResponse: NomisHistoryResponse, recordedAt: LocalDateTime): HistoricalResponse? =
    nomisResponse.answer?.let {
      HistoricalResponse(
        historicalQuestion = this,
        code = nomisResponse.questionResponseId.toString(),
        response = it,
        label = it,
        sequence = nomisResponse.responseSequence,
        responseDate = nomisResponse.responseDate,
        additionalInformation = nomisResponse.comment,
        recordedBy = nomisResponse.recordingStaff.username,
        recordedAt = recordedAt,
      )
    }

  fun addResponse(response: HistoricalResponse): HistoricalResponse {
    this.responses.add(response)
    return response
  }

  fun addResponse(
    code: String,
    response: String,
    label: String,
    sequence: Int,
    responseDate: LocalDate? = null,
    additionalInformation: String? = null,
    recordedBy: String,
    recordedAt: LocalDateTime,
  ): HistoricalQuestion {
    addResponse(
      HistoricalResponse(
        historicalQuestion = this,
        code = code,
        response = response,
        label = label,
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
    label = label,
    sequence = sequence,
    additionalInformation = additionalInformation,
    responses = responses.map { it.toDto() },
  )
}
