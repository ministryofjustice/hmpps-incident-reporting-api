package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Previous question with responses making up a previous version of an incident report", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class HistoricalQuestion(
  @Schema(description = "The question code")
  val code: String,
  @Schema(description = "The question")
  val question: String,
  @Schema(description = "The responses to this question")
  val responses: List<HistoricalResponse> = emptyList(),
  @Schema(description = "Optional additional information", nullable = true)
  val additionalInformation: String? = null,
)
