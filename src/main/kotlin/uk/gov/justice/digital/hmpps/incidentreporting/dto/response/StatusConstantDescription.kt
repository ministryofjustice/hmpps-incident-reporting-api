package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Report status constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StatusConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", example = "IN_ANALYSIS")
  val code: String,
  @Schema(description = "Human-readable description of this value", example = "In analysis")
  val description: String,

  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @Schema(
    description = "Machine-readable NOMIS identifier of this value, " +
      "which may be null for statuses that cannot be mapped",
    nullable = true,
    example = "INAN",
    deprecated = true,
  )
  val nomisCode: String?,
)
