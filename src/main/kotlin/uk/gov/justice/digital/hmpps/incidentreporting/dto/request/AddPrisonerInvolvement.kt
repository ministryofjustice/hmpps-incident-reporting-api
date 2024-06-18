package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole

@Schema(description = "Add an involved prisoner to an incident report")
data class AddPrisonerInvolvement(
  @Schema(description = "Prisoner’s NOMIS number", required = true)
  val prisonerNumber: String,
  @Schema(description = "Their role", required = true)
  val prisonerRole: PrisonerRole,
  @Schema(description = "Optional outcome of prisoner’s involvement", required = false, defaultValue = "null")
  val outcome: PrisonerOutcome? = null,
  @Schema(description = "Optional comment on prisoner’s involvement", required = false, defaultValue = "null")
  val comment: String? = null,
)
