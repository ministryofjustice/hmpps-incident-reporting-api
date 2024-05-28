package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CorrectionRequest as CorrectionRequestDto

@Entity
@EntityOpen
class CorrectionRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  val reason: CorrectionReason,
  val descriptionOfChange: String,

  val correctionRequestedBy: String,
  val correctionRequestedAt: LocalDateTime,
) {
  override fun toString(): String {
    return "CorrectionRequest(id=$id)"
  }

  fun getReport() = report

  fun toDto() = CorrectionRequestDto(
    reason = reason,
    descriptionOfChange = descriptionOfChange,
    correctionRequestedBy = correctionRequestedBy,
    correctionRequestedAt = correctionRequestedAt,
  )
}
