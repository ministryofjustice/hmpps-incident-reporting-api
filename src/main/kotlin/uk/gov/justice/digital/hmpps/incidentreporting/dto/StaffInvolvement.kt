package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole

@Schema(description = "Member of staff involved in an incident", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StaffInvolvement(
  @Schema(description = "Username")
  val staffUsername: String,
  @Schema(description = "Their role")
  val staffRole: StaffRole,
  @Schema(description = "Optional comment on staff member involvement", nullable = true)
  val comment: String? = null,
)
