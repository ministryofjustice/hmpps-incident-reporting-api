package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Question with responses making up an incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class Question(
  @Schema(description = "The question code", required = true)
  val code: String,
  @Schema(description = "The question", required = true)
  val question: String,
  @Schema(description = "The responses to this question", required = true)
  val responses: List<Response> = emptyList(),
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)
