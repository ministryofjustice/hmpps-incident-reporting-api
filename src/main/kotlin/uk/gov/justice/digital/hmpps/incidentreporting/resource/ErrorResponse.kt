package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus

@Schema(description = "Error response")
data class ErrorResponse(
  @Schema(description = "HTTP status code", example = "500", required = true)
  val status: Int,
  @Schema(description = "User message for the error", example = "No incident report found for ID `55544222`", required = true)
  val userMessage: String,
  @Schema(description = "More detailed error message", example = "[Details, sometimes a stack trace]", required = true)
  val developerMessage: String,
  @Schema(description = "When present, uniquely identifies the type of error making it easier for clients to discriminate without relying on error description or HTTP status code; see `uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorCode` enumeration in hmpps-incident-reporting-api", example = "101", required = false)
  val errorCode: Int? = null,
  @Schema(description = "More information about the error", example = "[Rarely used, error-specific]", required = false)
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    userMessage: String,
    developerMessage: String? = null,
    errorCode: ErrorCode? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), userMessage, developerMessage ?: userMessage, errorCode?.errorCode, moreInfo)
}
