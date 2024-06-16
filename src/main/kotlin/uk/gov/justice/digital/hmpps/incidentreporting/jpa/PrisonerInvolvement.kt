package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdatePrisonerInvolvement
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement as PrisonerInvolvementDto

@Entity
class PrisonerInvolvement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  var prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  var prisonerRole: PrisonerRole,

  @Enumerated(EnumType.STRING)
  var outcome: PrisonerOutcome? = null,

  var comment: String? = null,
) {
  override fun toString(): String {
    return "PrisonerInvolvement(id=$id)"
  }

  fun getReport() = report

  fun updateWith(request: UpdatePrisonerInvolvement) {
    request.prisonerNumber?.let { prisonerNumber = it }
    request.prisonerRole?.let { prisonerRole = it }
    request.outcome?.let { outcome = it.getOrNull() }
    request.comment?.let { comment = it.getOrNull() }
  }

  fun toDto() = PrisonerInvolvementDto(
    prisonerNumber = prisonerNumber,
    prisonerRole = prisonerRole,
    outcome = outcome,
    comment = comment,
  )
}
