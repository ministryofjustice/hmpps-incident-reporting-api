package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Incident type constant")
data class TypeConstantDescription(
  @Schema(description = "Machine-readable identifier of this value", required = true, readOnly = true, example = "DISORDER")
  val code: String,
  @Schema(description = "Human-readable description of this value", required = true, readOnly = true, example = "Disorder")
  val description: String,
  @Schema(description = "Whether this type is currently active and usable in new reports", required = false, readOnly = true, defaultValue = "true", example = "true")
  val active: Boolean = true,

  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @Schema(description = "Machine-readable NOMIS identifier of this value, which may be null for newer incident types", required = false, readOnly = true, example = "DISORDER1", deprecated = true)
  val nomisCode: String?,
)
