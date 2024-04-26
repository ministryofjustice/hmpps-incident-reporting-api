package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CorrectionRequest as CorrectionRequestDto

@Entity
class CorrectionRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val report: Report,

  val reason: CorrectionReason,
  val descriptionOfChange: String,

  val correctionRequestedBy: String,
  val correctionRequestedAt: LocalDateTime,
) {
  fun toDto() = CorrectionRequestDto(
    reason = reason,
    descriptionOfChange = descriptionOfChange,
    correctionRequestedBy = correctionRequestedBy,
    correctionRequestedAt = correctionRequestedAt,
  )
}
