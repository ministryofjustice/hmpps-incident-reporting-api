package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "Payload to add question with responses to an incident report")
data class AddQuestionWithResponses(
  @Schema(description = "The question code", required = true)
  @field:Size(max = 60)
  val code: String,
  @Schema(description = "The question", required = true)
  @field:Size(min = 1)
  val question: String,
  @Schema(description = "The responses to this question", required = true)
  @field:Valid
  @field:Size(min = 1)
  val responses: List<AddQuestionResponse>,
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)

data class AddQuestionResponse(
  @Schema(description = "The response", required = true)
  @field:Size(min = 1)
  val response: String,
  @Schema(description = "Username of person who responded to the question", required = true)
  @field:Size(min = 3, max = 120)
  val recordedBy: String,
  @Schema(description = "When the response was made", required = true, example = "2024-04-29T12:34:56.789012")
  val recordedAt: LocalDateTime,
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)
