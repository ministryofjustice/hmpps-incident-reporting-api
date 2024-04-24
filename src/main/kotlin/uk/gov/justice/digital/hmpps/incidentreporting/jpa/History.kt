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
import java.time.LocalDateTime

@Entity
class History(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  @Enumerated(EnumType.STRING)
  val type: Type,

  val changeDate: LocalDateTime,
  val changeStaffUsername: String,

  @OneToMany(mappedBy = "history", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val questions: MutableList<HistoricalQuestion> = mutableListOf(),
) {

  fun addQuestion(
    dataItem: String,
    dataItemDescription: String? = null,
  ): HistoricalQuestion {
    val historicalQuestion = HistoricalQuestion(
      history = this,
      dataItem = dataItem,
      dataItemDescription = dataItemDescription,
    )
    questions.add(historicalQuestion)
    return historicalQuestion
  }

  fun getReport() = report

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as History

    if (report != other.report) return false
    if (type != other.type) return false
    if (changeDate != other.changeDate) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + changeDate.hashCode()
    return result
  }
}
