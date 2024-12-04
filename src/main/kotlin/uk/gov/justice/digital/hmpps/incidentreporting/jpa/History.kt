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
import org.hibernate.Hibernate
import org.hibernate.annotations.SortNatural
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistory
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisHistoryQuestion
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDateTime
import java.util.SortedSet
import uk.gov.justice.digital.hmpps.incidentreporting.dto.History as HistoryDto

@Entity
@EntityOpen
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

  @OneToMany(mappedBy = "history", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val questions: SortedSet<HistoricalQuestion> = sortedSetOf(),
) : Comparable<History> {

  companion object {
    private val COMPARATOR = compareBy<History>
      { it.report }
      .thenBy { it.changedAt }
      .thenBy { it.type }
  }

  override fun compareTo(other: History) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as History

    if (report != other.report) return false
    if (changedAt != other.changedAt) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + changedAt.hashCode()
    result = 31 * result + type.hashCode()
    return result
  }

  fun findQuestion(code: String, sequence: Int) = this.questions.firstOrNull { it.code == code && it.sequence == sequence }

  fun updateOrAddHistoryQuestions(
    history: NomisHistory,
    recordedAt: LocalDateTime,
  ) {
    this.questions.retainAll(
      history.questions.map { nomisQuestion ->
        val question = this.updateOrAddQuestion(nomisQuestion)
        question.updateResponses(nomisQuestion.answers, recordedAt)
        question
      }.toSet(),
    )
  }

  fun updateOrAddQuestion(
    nomisQuestion: NomisHistoryQuestion,
  ) =
    findQuestion(
      code = nomisQuestion.questionId.toString(),
      sequence = nomisQuestion.sequence - 1,
    )?.apply {
      question = nomisQuestion.question
    } ?: addNomisHistoryQuestion(nomisQuestion).also { newQuestion ->
      questions.add(newQuestion)
    }

  private fun addNomisHistoryQuestion(question: NomisHistoryQuestion) =
    this.addQuestion(
      code = question.questionId.toString(),
      sequence = question.sequence - 1,
      question = question.question,
    )

  fun addQuestion(
    code: String,
    question: String,
    sequence: Int,
    additionalInformation: String? = null,
  ): HistoricalQuestion {
    return HistoricalQuestion(
      history = this,
      code = code,
      question = question,
      sequence = sequence,
      additionalInformation = additionalInformation,
    ).also { questions.add(it) }
  }

  fun toDto() = HistoryDto(
    type = type,
    changedAt = changedAt,
    changedBy = changedBy,
    questions = questions.map { it.toDto() },
  )

  override fun toString(): String {
    return "History(report=$report, type=$type, changedAt=$changedAt)"
  }
}
