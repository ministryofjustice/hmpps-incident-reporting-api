package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import java.util.Optional

@Schema(description = "Update an involved member of staff in an incident report")
data class UpdateStaffInvolvement(
  @Schema(description = "Username", required = false, defaultValue = "null")
  val staffUsername: String? = null,
  @Schema(description = "Their role", required = false, defaultValue = "null")
  val staffRole: StaffRole? = null,
  @Schema(description = "Optional comment on staff member involvement", required = false, defaultValue = "null")
  val comment: Optional<String>? = null,
)