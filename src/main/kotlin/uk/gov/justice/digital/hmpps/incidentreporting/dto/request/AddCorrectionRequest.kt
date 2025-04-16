package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Add a correction request to an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class AddCorrectionRequest(
  @Schema(description = "The changes being requested", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Size(min = 1)
  val descriptionOfChange: String,
  @Schema(
    description = "The location where the staff member is raising the correction",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    minLength = 3,
  )
  val location: String? = null,
)
