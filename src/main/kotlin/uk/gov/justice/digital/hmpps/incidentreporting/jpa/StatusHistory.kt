package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
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
) {
  override fun toString(): String {
    return "StatusHistory(id=$id)"
  }

  fun getReport() = report

  fun toDto() = StatusHistoryDto(
    status = status,
    changedAt = setOn,
    changedBy = setBy,
  )
}
