package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User action constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class UserTypeConstantDescription(
  @param:Schema(description = "Machine-readable identifier of this value", example = "REPORTING_OFFICER")
  val code: String,
  @param:Schema(description = "Human-readable description of this value", example = "Request correction")
  val description: String,
)
