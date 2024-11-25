package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Payload to add question with responses to an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class AddQuestionWithResponses(
  @Schema(description = "The question code", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 60)
  @field:Size(min = 1, max = 60)
  val code: String,
  @Schema(description = "The question text as seen by downstream data consumers", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Size(min = 1)
  val question: String,
  @Schema(description = "The responses to this question", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Valid
  @field:Size(min = 1)
  val responses: List<AddQuestionResponse>,
  @Schema(description = "Optional additional information", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null")
  val additionalInformation: String? = null,
)

@Schema(description = "A response to a question", accessMode = Schema.AccessMode.WRITE_ONLY)
data class AddQuestionResponse(
  @Schema(description = "The response", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Size(min = 1)
  val response: String,
  @Schema(description = "Optional response as a date", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null", example = "2024-04-29")
  val responseDate: LocalDate? = null,
  @Schema(description = "Optional additional information", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true, defaultValue = "null")
  val additionalInformation: String? = null,
)
