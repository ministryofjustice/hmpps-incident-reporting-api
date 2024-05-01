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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StaffInvolvement as StaffInvolvementDto

@Entity
class StaffInvolvement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  val staffUsername: String,

  @Enumerated(EnumType.STRING)
  val staffRole: StaffRole,

  val comment: String? = null,
) : DtoConvertible {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as StaffInvolvement

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "StaffInvolvement(id=$id)"
  }

  fun getReport() = report

  override fun toDto() = StaffInvolvementDto(
    staffUsername = staffUsername,
    staffRole = staffRole,
    comment = comment,
  )
}
