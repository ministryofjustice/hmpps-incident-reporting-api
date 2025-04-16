package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
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
  val report: Report,

  val sequence: Int,

  var descriptionOfChange: String,
  var correctionRequestedBy: String,
  var correctionRequestedAt: LocalDateTime,
  var location: String? = null,
) : Comparable<CorrectionRequest> {

  companion object {
    private val COMPARATOR = compareBy<CorrectionRequest>
      { it.report }
      .thenBy { it.sequence }
  }

  override fun compareTo(other: CorrectionRequest) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as CorrectionRequest

    if (report != other.report) return false
    if (sequence != other.sequence) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + sequence.hashCode()
    return result
  }

  override fun toString(): String {
    return "CorrectionRequest(id=$id, reportReference=${report.reportReference}, " +
      "correctionRequestedAt=$correctionRequestedAt, descriptionOfChange=$descriptionOfChange)"
  }

  fun updateWith(
    request: UpdateCorrectionRequest,
    requestUsername: String,
    now: LocalDateTime,
  ) {
    request.descriptionOfChange?.let { descriptionOfChange = it }
    request.location?.let { location = it }
    correctionRequestedBy = requestUsername
    correctionRequestedAt = now
  }

  fun toDto() = CorrectionRequestDto(
    sequence = sequence,
    descriptionOfChange = descriptionOfChange,
    correctionRequestedBy = correctionRequestedBy,
    correctionRequestedAt = correctionRequestedAt,
    location = location,
  )
}
