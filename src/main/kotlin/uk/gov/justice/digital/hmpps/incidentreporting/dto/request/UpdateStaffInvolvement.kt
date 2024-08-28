package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import java.util.Optional

@Schema(description = "Update an involved member of staff in an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class UpdateStaffInvolvement(
  @Schema(description = "Username", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null", minLength = 3, maxLength = 120)
  @field:Size(min = 3, max = 120)
  val staffUsername: String? = null,
  @Schema(description = "Their role", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null")
  val staffRole: StaffRole? = null,
  @Schema(description = "Optional comment on staff member involvement – omit to preserve existing comment, provide null to clear it", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  val comment: Optional<String>? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    staffUsername == null && staffRole == null && comment == null
}
