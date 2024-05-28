package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalQuestion as HistoricalQuestionDto

@Entity
@EntityOpen
class HistoricalQuestion(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val history: History,

  val code: String,
  val question: String,

  val additionalInformation: String? = null,

  @OneToMany(mappedBy = "historicalQuestion", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence", nullable = false)
  private val responses: MutableList<HistoricalResponse> = mutableListOf(),
) {
  override fun toString(): String {
    return "HistoricalQuestion(id=$id)"
  }

  fun getResponses(): List<HistoricalResponse> = responses

  fun addResponse(
    response: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedAt: LocalDateTime,
  ): HistoricalQuestion {
    responses.add(
      HistoricalResponse(
        historicalQuestion = this,
        response = response,
        recordedBy = recordedBy,
        recordedAt = recordedAt,
        additionalInformation = additionalInformation,
      ),
    )
    return this
  }

  fun toDto() = HistoricalQuestionDto(
    code = code,
    question = question,
    additionalInformation = additionalInformation,
    responses = responses.map { it.toDto() },
  )
}
