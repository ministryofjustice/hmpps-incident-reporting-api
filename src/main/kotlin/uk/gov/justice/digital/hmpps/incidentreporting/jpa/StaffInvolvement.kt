package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StaffInvolvement as StaffInvolvementDto

@Entity
@EntityOpen
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
) {
  override fun toString(): String {
    return "StaffInvolvement(id=$id)"
  }

  fun getReport() = report

  fun toDto() = StaffInvolvementDto(
    staffUsername = staffUsername,
    staffRole = staffRole,
    comment = comment,
  )
}
