package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import java.util.Optional

@Schema(description = "Update an involved member of staff in an incident report")
data class UpdateStaffInvolvement(
  @Schema(description = "Username", required = false, defaultValue = "null")
  @field:Size(min = 3, max = 120)
  val staffUsername: String? = null,
  @Schema(description = "Their role", required = false, defaultValue = "null")
  val staffRole: StaffRole? = null,
  @Schema(description = "Optional comment on staff member involvement – omit to preserve existing comment, provide null to clear it", required = false, defaultValue = "null")
  val comment: Optional<String>? = null,
) {
  val isEmpty: Boolean =
    staffUsername == null && staffRole == null && comment == null
}
