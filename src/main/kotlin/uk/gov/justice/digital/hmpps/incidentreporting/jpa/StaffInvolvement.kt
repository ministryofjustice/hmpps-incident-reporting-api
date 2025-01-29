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
  val report: Report,

  val sequence: Int,

  var staffUsername: String,
  var firstName: String,
  var lastName: String,

  @Enumerated(EnumType.STRING)
  var staffRole: StaffRole,

  var comment: String? = null,
) : Comparable<StaffInvolvement> {

  companion object {
    private val COMPARATOR = compareBy<StaffInvolvement>
      { it.report }
      .thenBy { it.sequence }
  }

  override fun compareTo(other: StaffInvolvement) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as StaffInvolvement

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
    return "StaffInvolvement(id=$id, reportReference=${report.reportReference}, " +
      "staffUsername=$staffUsername, staffRole=$staffRole, comment=$comment)"
  }

  fun updateWith(request: UpdateStaffInvolvement) {
    request.staffUsername?.let { staffUsername = it }
    request.firstName?.let { firstName = it }
    request.lastName?.let { lastName = it }
    request.staffRole?.let { staffRole = it }
    request.comment?.let { comment = it.getOrNull() }
  }

  fun toDto() = StaffInvolvementDto(
    sequence = sequence,
    staffUsername = staffUsername,
    firstName = firstName,
    lastName = lastName,
    staffRole = staffRole,
    comment = comment,
  )
}
