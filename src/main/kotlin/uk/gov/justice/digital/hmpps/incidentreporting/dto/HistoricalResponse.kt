package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Previous response to a question making up a previous version of an incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class HistoricalResponse(
  @Schema(description = "The response", required = true)
  val response: String,
  @Schema(description = "Username of person who responded to the question", required = true)
  val recordedBy: String,
  @Schema(description = "When the response was made", required = true, example = "2024-04-29T12:34:56.789012")
  val recordedAt: LocalDateTime,
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)
