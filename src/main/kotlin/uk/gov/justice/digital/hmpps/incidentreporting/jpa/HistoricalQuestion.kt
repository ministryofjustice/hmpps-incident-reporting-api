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
import org.hibernate.Hibernate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalQuestion as HistoricalQuestionDto

@Entity
class HistoricalQuestion(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val history: History,

  override val code: String,
  // TODO: should we force `question` to be non-null?
  override val question: String? = null,

  @OneToMany(mappedBy = "historicalQuestion", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence")
  private val responses: MutableList<HistoricalResponse> = mutableListOf(),
) : DtoConvertible, GenericQuestion {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as HistoricalQuestion

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "HistoricalQuestion(id=$id)"
  }

  override fun getResponses(): List<HistoricalResponse> = responses

  override fun addResponse(
    response: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): HistoricalQuestion {
    responses.add(
      HistoricalResponse(
        historicalQuestion = this,
        response = response,
        recordedBy = recordedBy,
        recordedOn = recordedOn,
        additionalInformation = additionalInformation,
      ),
    )
    return this
  }

  override fun toDto() = HistoricalQuestionDto(
    code = code,
    question = question,
    responses = responses.map { it.toDto() },
  )
}
