package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description = "Code and description of a constant or enumeration member",
  accessMode = Schema.AccessMode.READ_ONLY,
)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class ConstantDescription(
  @param:Schema(description = "Machine-readable identifier of this value", example = "VICTIM")
  val code: String,
  @param:Schema(description = "Human-readable description of this value", example = "Victim")
  val description: String,
)
