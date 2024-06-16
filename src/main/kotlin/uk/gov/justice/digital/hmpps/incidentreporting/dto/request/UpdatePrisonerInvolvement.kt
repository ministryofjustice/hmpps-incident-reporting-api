package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import java.util.Optional

@Schema(description = "Update an involved prisoner in an incident report")
data class UpdatePrisonerInvolvement(
  @Schema(description = "Prisoner’s NOMIS number", required = false, defaultValue = "null")
  val prisonerNumber: String? = null,
  @Schema(description = "Their role", required = false, defaultValue = "null")
  val prisonerRole: PrisonerRole? = null,
  @Schema(description = "Optional outcome of prisoner’s involvement", required = false, defaultValue = "null")
  val outcome: Optional<PrisonerOutcome>? = null,
  @Schema(description = "Optional comment on prisoner’s involvement", required = false, defaultValue = "null")
  val comment: Optional<String>? = null,
)