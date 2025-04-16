package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Update a correction request in an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class UpdateCorrectionRequest(
  @Schema(
    description = "The changes being requested",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 1,
  )
  @field:Size(min = 1)
  val descriptionOfChange: String? = null,
  @Schema(
    description = "The location where the staff member is raising the correction",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 3,
  )
  val location: String? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    descriptionOfChange == null
}
