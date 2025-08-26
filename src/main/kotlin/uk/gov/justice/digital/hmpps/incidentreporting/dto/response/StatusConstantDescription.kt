package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Report status constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StatusConstantDescription(
  @param:Schema(description = "Machine-readable identifier of this value", example = "ON_HOLD")
  val code: String,
  @param:Schema(description = "Human-readable description of this value", example = "On hold")
  val description: String,
  @param:Schema(
    description = "Whether reports with this status should be ignored downstream for most statistical purposes",
    example = "false",
  )
  val ignoreDownstream: Boolean,

  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @param:Schema(
    description = "Machine-readable NOMIS identifier of this value, " +
      "which may be null for statuses that cannot be mapped",
    nullable = true,
    example = "INAN",
    deprecated = true,
  )
  val nomisCode: String?,
)
