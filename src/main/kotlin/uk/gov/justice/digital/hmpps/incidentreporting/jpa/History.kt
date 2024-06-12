package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import org.hibernate.annotations.BatchSize
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.History as HistoryDto

@Entity
class History(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  @Enumerated(EnumType.STRING)
  val type: Type,

  val changedAt: LocalDateTime,
  val changedBy: String,

  @OneToMany(mappedBy = "history", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence", nullable = false)
  @BatchSize(size = 50)
  val questions: MutableList<HistoricalQuestion> = mutableListOf(),
) {
  override fun toString(): String {
    return "History(id=$id)"
  }

  fun getReport() = report

  fun addQuestion(
    code: String,
    question: String,
    additionalInformation: String? = null,
  ): HistoricalQuestion {
    return HistoricalQuestion(
      history = this,
      code = code,
      question = question,
      additionalInformation = additionalInformation,
    ).also { questions.add(it) }
  }

  fun toDto() = HistoryDto(
    type = type,
    changedAt = changedAt,
    changedBy = changedBy,
    questions = questions.map { it.toDto() },
  )
}
