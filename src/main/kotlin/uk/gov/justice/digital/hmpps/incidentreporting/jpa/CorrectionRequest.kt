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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateCorrectionRequest
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

  @Enumerated(EnumType.STRING)
  var reason: CorrectionReason,
  var descriptionOfChange: String,
  var correctionRequestedBy: String,
  var correctionRequestedAt: LocalDateTime,
) : Comparable<CorrectionRequest> {

  companion object {
    private val COMPARATOR = compareBy<CorrectionRequest>
      { it.report }
      .thenBy { it.correctionRequestedAt }
      .thenBy { it.reason }
      .thenBy { it.descriptionOfChange }
  }

  override fun compareTo(other: CorrectionRequest) = COMPARATOR.compare(this, other)

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as CorrectionRequest

    if (report != other.report) return false
    if (correctionRequestedAt != other.correctionRequestedAt) return false
    if (reason != other.reason) return false
    if (descriptionOfChange != other.descriptionOfChange) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + correctionRequestedAt.hashCode()
    result = 31 * result + reason.hashCode()
    result = 31 * result + descriptionOfChange.hashCode()
    return result
  }

  override fun toString(): String {
    return "CorrectionRequest(report=$report, reason=$reason, descriptionOfChange='$descriptionOfChange', correctionRequestedAt=$correctionRequestedAt)"
  }
}
