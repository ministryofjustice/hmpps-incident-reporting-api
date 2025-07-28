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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserType
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateCorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull
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
  @Enumerated(EnumType.STRING)
  var userAction: UserAction? = null,
  var originalReportReference: String? = null,
  @Enumerated(EnumType.STRING)
  var userType: UserType? = null,
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
      "descriptionOfChange=$descriptionOfChange, location=$location, " +
      "correctionRequestedBy=$correctionRequestedBy, correctionRequestedAt=$correctionRequestedAt)"
  }

  fun updateWith(
    request: UpdateCorrectionRequest,
    requestUsername: String,
    now: LocalDateTime,
  ) {
    request.descriptionOfChange?.let { descriptionOfChange = it }
    request.location?.let { location = it.getOrNull() }
    correctionRequestedBy = requestUsername
    correctionRequestedAt = now
    request.userAction?.let { userAction = it.getOrNull() }
    request.originalReportReference?.let { originalReportReference = it.getOrNull() }
    request.userType?.let { userType = it.getOrNull() }
  }

  fun toDto() = CorrectionRequestDto(
    sequence = sequence,
    descriptionOfChange = descriptionOfChange,
    correctionRequestedBy = correctionRequestedBy,
    correctionRequestedAt = correctionRequestedAt,
    location = location,
    userType = userType,
    originalReportReference = originalReportReference,
    userAction = userAction,
  )
}
