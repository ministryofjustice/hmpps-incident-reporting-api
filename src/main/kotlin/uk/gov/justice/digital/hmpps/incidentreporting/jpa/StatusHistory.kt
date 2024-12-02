package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StatusHistory as StatusHistoryDto

@Entity
@EntityOpen
class StatusHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  @Enumerated(EnumType.STRING)
  val status: Status,

  val changedAt: LocalDateTime,
  val changedBy: String,
) : Comparable<StatusHistory> {

  fun toDto() = StatusHistoryDto(
    status = status,
    changedAt = changedAt,
    changedBy = changedBy,
  )

  companion object {
    private val COMPARATOR = compareBy<StatusHistory>
      { it.changedAt }
      .thenBy { it.report.id }
      .thenBy { it.status }
  }

  override fun compareTo(other: StatusHistory) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as StatusHistory

    if (changedAt != other.changedAt) return false
    if (report != other.report) return false
    if (status != other.status) return false

    return true
  }

  override fun hashCode(): Int {
    var result = changedAt.hashCode()
    result = 31 * result + report.hashCode()
    result = 31 * result + status.hashCode()
    return result
  }

  override fun toString(): String {
    return "StatusHistory(report=$report, status=$status, changedAt=$changedAt)"
  }
}
