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
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StatusHistory as StatusHistoryDto

@Entity
class StatusHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  @Enumerated(EnumType.STRING)
  val status: Status,

  val setOn: LocalDateTime,
  val setBy: String,
) : DtoConvertible {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as StatusHistory

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "StatusHistory(id=$id)"
  }

  fun getReport() = report

  override fun toDto() = StatusHistoryDto(
    status = status,
    setOn = setOn,
    setBy = setBy,
  )
}
