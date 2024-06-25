package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole

@Schema(description = "Add an involved member of staff to an incident report")
data class AddStaffInvolvement(
  @Schema(description = "Username", required = true, minLength = 3, maxLength = 120)
  @field:Size(min = 3, max = 120)
  val staffUsername: String,
  @Schema(description = "Their role", required = true)
  val staffRole: StaffRole,
  @Schema(description = "Optional comment on staff member involvement", required = false, defaultValue = "null")
  val comment: String? = null,
)
