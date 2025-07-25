package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User type constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class UserActionConstantDescription(
  @param:Schema(description = "Machine-readable identifier of this value", example = "REQUEST_CORRECTION")
  val code: String,
  @param:Schema(description = "Human-readable description of this value", example = "Reporting officer")
  val description: String,
)
