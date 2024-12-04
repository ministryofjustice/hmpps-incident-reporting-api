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
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateStaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StaffInvolvement as StaffInvolvementDto

@Entity
@EntityOpen
class StaffInvolvement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  var staffUsername: String,

  @Enumerated(EnumType.STRING)
  var staffRole: StaffRole,

  var comment: String? = null,
) : Comparable<StaffInvolvement> {

  companion object {
    private val COMPARATOR = compareBy<StaffInvolvement>
      { it.report }
      .thenBy { it.staffUsername }
      .thenBy { it.staffRole }
      .thenBy(nullsLast()) { it.comment }
  }

  override fun compareTo(other: StaffInvolvement) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as StaffInvolvement

    if (report != other.report) return false
    if (staffUsername != other.staffUsername) return false
    if (staffRole != other.staffRole) return false
    if (comment != other.comment) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + staffUsername.hashCode()
    result = 31 * result + staffRole.hashCode()
    result = 31 * result + (comment?.hashCode() ?: 0)
    return result
  }

  fun updateWith(request: UpdateStaffInvolvement) {
    request.staffUsername?.let { staffUsername = it }
    request.staffRole?.let { staffRole = it }
    request.comment?.let { comment = it.getOrNull() }
  }

  fun toDto() = StaffInvolvementDto(
    staffUsername = staffUsername,
    staffRole = staffRole,
    comment = comment,
  )

  override fun toString(): String {
    return "StaffInvolvement(report=$report, staffUsername='$staffUsername', staffRole=$staffRole)"
  }
}
