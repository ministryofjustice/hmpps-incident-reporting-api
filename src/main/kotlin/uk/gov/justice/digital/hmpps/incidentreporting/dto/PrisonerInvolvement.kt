package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole

data class PrisonerInvolvement(
  val prisonerNumber: String,
  val prisonerInvolvement: PrisonerRole,
  val outcome: PrisonerOutcome? = null,
  val comment: String? = null,
)
