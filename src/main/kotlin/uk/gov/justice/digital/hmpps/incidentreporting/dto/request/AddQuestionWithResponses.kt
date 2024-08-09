package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Payload to add question with responses to an incident report")
data class AddQuestionWithResponses(
  @Schema(description = "The question code", required = true, minLength = 1, maxLength = 60)
  @field:Size(min = 1, max = 60)
  val code: String,
  @Schema(description = "The question", required = true, minLength = 1)
  @field:Size(min = 1)
  val question: String,
  @Schema(description = "The responses to this question", required = true, minLength = 1)
  @field:Valid
  @field:Size(min = 1)
  val responses: List<AddQuestionResponse>,
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)

data class AddQuestionResponse(
  @Schema(description = "The response", required = true, minLength = 1)
  @field:Size(min = 1)
  val response: String,
  @Schema(description = "Optional response as a date", required = false, example = "2024-04-29")
  val responseDate: LocalDate? = null,
  @Schema(description = "Optional additional information", required = false, defaultValue = "null")
  val additionalInformation: String? = null,
)
