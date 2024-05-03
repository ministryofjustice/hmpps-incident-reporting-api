package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Previous question with responses making up a previous version of an incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class HistoricalQuestion(
  @Schema(description = "The question code", required = true)
  val code: String,
  @Schema(description = "The question", required = false, defaultValue = "null")
  val question: String? = null,
  @Schema(description = "The responses to this question", required = true)
  val responses: List<HistoricalResponse> = emptyList(),
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)
