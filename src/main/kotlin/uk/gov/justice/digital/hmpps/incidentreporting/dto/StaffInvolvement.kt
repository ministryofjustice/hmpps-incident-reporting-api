package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole

@Schema(description = "Member of staff involved in an incident")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StaffInvolvement(
  @Schema(description = "Username", required = true)
  val staffUsername: String,
  @Schema(description = "Their role", required = true)
  val staffRole: StaffRole,
  @Schema(description = "Optional comment on staff member involvement", required = false, defaultValue = "null")
  val comment: String? = null,
)
