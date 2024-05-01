package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Where an incident took place")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class Location(
  @Schema(description = "NOMIS id of location", required = true)
  val locationId: String,
  @Schema(description = "Type of location", required = true)
  val type: String,
  @Schema(description = "Description of location", required = true)
  val description: String,
) : Dto
