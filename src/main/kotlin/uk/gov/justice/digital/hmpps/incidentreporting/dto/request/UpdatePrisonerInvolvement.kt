package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import java.util.Optional

@Schema(description = "Update an involved prisoner in an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class UpdatePrisonerInvolvement(
  @Schema(description = "Prisoner’s NOMIS number", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null", minLength = 7, maxLength = 10)
  @field:Size(min = 7, max = 10)
  val prisonerNumber: String? = null,
  @Schema(description = "First name", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null", minLength = 1, maxLength = 255)
  @field:Size(min = 1, max = 255)
  val firstName: String? = null,
  @Schema(description = "Surname", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null", minLength = 1, maxLength = 255)
  @field:Size(min = 1, max = 255)
  val lastName: String? = null,
  @Schema(description = "Their role", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null")
  val prisonerRole: PrisonerRole? = null,
  @Schema(description = "Optional outcome of prisoner’s involvement – omit to preserve existing outcome, provide null to clear it", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  val outcome: Optional<PrisonerOutcome>? = null,
  @Schema(description = "Optional comment on prisoner’s involvement – omit to preserve existing comment, provide null to clear it", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  val comment: Optional<String>? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    prisonerNumber == null &&
      firstName == null &&
      lastName == null &&
      prisonerRole == null &&
      outcome == null &&
      comment == null
}
