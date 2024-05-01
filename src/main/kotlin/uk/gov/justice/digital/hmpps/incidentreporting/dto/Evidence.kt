package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Evidence supporting an incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class Evidence(
  @Schema(description = "Type of evidence", required = true)
  val type: String,
  @Schema(description = "Description of evidence", required = true)
  val description: String,
) : Dto
