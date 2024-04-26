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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement as PrisonerInvolvementDto

@Entity
class PrisonerInvolvement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  val prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  val prisonerInvolvement: PrisonerRole,

  @Enumerated(EnumType.STRING)
  val outcome: PrisonerOutcome? = null,

  val comment: String? = null,
) {
  fun getReport() = report

  fun toDto() = PrisonerInvolvementDto(
    prisonerNumber = prisonerNumber,
    prisonerInvolvement = prisonerInvolvement,
    outcome = outcome,
    comment = comment,
  )
}
