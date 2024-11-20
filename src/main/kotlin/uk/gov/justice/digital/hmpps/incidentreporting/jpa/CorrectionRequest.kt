package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateCorrectionRequest
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CorrectionRequest as CorrectionRequestDto

@Entity
class CorrectionRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  // TODO: likely to be removed
  @Enumerated(EnumType.STRING)
  var reason: CorrectionReason,
  var descriptionOfChange: String,
  var correctionRequestedBy: String,
  var correctionRequestedAt: LocalDateTime,
) {
  override fun toString(): String {
    return "CorrectionRequest(id=$id)"
  }

  fun getReport() = report

  fun updateWith(request: UpdateCorrectionRequest, requestUsername: String, now: LocalDateTime) {
    request.reason?.let { reason = it }
    request.descriptionOfChange?.let { descriptionOfChange = it }
    correctionRequestedBy = requestUsername
    correctionRequestedAt = now
  }

  fun toDto() = CorrectionRequestDto(
    reason = reason,
    descriptionOfChange = descriptionOfChange,
    correctionRequestedBy = correctionRequestedBy,
    correctionRequestedAt = correctionRequestedAt,
  )
}
