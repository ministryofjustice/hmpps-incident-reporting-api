package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Question with responses making up an incident report", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class Question(
  @Schema(description = "The question code; used as a unique identifier within one report and typically refers to a specific question for an incident type")
  val code: String,
  @Schema(description = "The question text as seen by downstream data consumers")
  val question: String,
  @Schema(description = "Sequence of the questions")
  val sequence: Int,
  @Schema(description = "The responses to this question")
  val responses: List<Response> = emptyList(),
  @Schema(description = "Optional additional information", nullable = true)
  val additionalInformation: String? = null,
)
