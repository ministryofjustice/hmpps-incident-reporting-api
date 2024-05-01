package uk.gov.justice.digital.hmpps.incidentreporting.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Question with responses making up an incident report")
data class Question(
  @Schema(description = "The question code", required = true)
  val code: String,
  @Schema(description = "The question", required = false, defaultValue = "null")
  val question: String? = null,
  @Schema(description = "The responses to this question", required = true)
  val responses: List<Response> = emptyList(),
) : Dto
