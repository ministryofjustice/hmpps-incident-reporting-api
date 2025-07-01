package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
  description = "Payload to add question with responses to an incident report or, " +
    "if the question code exists in the report, update the question and overwrite responses",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class AddOrUpdateQuestionWithResponses(
  @Schema(
    description = "The question code; used as a unique identifier within one report",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 1,
    maxLength = 60,
  )
  @field:Size(min = 1, max = 60)
  val code: String,
  @Schema(
    description = "The question text as seen by downstream data consumers",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 1,
  )
  @field:Size(min = 1)
  val question: String,
  @Schema(
    description = "The question text as seen by the user at the point of entry",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    minLength = 1,
  )
  @field:Size(min = 1)
  val label: String,
  @Schema(description = "The responses to this question", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Valid
  @field:Size(min = 1)
  val responses: List<AddOrUpdateQuestionResponse>,
  @Schema(
    description = "Optional additional information",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val additionalInformation: String? = null,
)

@Schema(description = "A response to a question", accessMode = Schema.AccessMode.WRITE_ONLY)
data class AddOrUpdateQuestionResponse(
  @Schema(
    description = "The response code; used as a unique identifier within one report",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    minLength = 1,
    maxLength = 60,
  )
  @field:Size(min = 1, max = 60)
  val code: String,
  @Schema(
    description = "The response text as seen by downstream data consumers",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 1,
  )
  @field:Size(min = 1)
  val response: String,
  @Schema(
    description = "The response text as seen by the user at the point of entry",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    minLength = 1,
  )
  @field:Size(min = 1)
  val label: String,
  @Schema(
    description = "Optional response as a date",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    example = "2024-04-29",
  )
  val responseDate: LocalDate? = null,
  @Schema(
    description = "Optional additional information",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val additionalInformation: String? = null,
)
