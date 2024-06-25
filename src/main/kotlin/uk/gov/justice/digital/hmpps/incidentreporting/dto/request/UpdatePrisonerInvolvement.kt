package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import java.util.Optional

@Schema(description = "Update an involved prisoner in an incident report")
data class UpdatePrisonerInvolvement(
  @Schema(description = "Prisoner’s NOMIS number", required = false, defaultValue = "null", minLength = 7, maxLength = 10)
  @field:Size(min = 7, max = 10)
  val prisonerNumber: String? = null,
  @Schema(description = "Their role", required = false, defaultValue = "null")
  val prisonerRole: PrisonerRole? = null,
  @Schema(description = "Optional outcome of prisoner’s involvement – omit to preserve existing outcome, provide null to clear it", required = false, defaultValue = "null")
  val outcome: Optional<PrisonerOutcome>? = null,
  @Schema(description = "Optional comment on prisoner’s involvement – omit to preserve existing comment, provide null to clear it", required = false, defaultValue = "null")
  val comment: Optional<String>? = null,
) {
  val isEmpty: Boolean =
    prisonerNumber == null && prisonerRole == null && outcome == null && comment == null
}
