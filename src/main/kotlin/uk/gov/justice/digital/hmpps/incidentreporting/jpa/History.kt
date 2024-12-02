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

  companion object {
    private val COMPARATOR = compareBy<History>
      { it.changedAt }
      .thenBy { it.report.id }
      .thenBy { it.type }
  }

  override fun compareTo(other: History) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as History

    if (changedAt != other.changedAt) return false
    if (report != other.report) return false
    if (type != other.type) return false

    return true
  }

  override fun hashCode(): Int {
    var result = changedAt.hashCode()
    result = 31 * result + report.hashCode()
    result = 31 * result + type.hashCode()
    return result
  }

  override fun toString(): String {
    return "History(report=$report, type=$type, changedAt=$changedAt)"
  }
}
