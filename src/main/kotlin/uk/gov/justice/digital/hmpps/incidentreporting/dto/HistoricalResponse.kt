package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
  description = "Previous response to a question making up a previous version of an incident report",
  accessMode = Schema.AccessMode.READ_ONLY,
)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class HistoricalResponse(
  @param:Schema(description = "Unique identifier for a response to a question", nullable = true)
  val code: String?,
  @param:Schema(description = "The response text as seen by downstream data consumers")
  val response: String,
  @param:Schema(description = "The response text as seen by the user at the point of entry")
  val label: String,
  // TODO: sequences are only being exposed while we sort out sync problems: they do not need to remain in the api contract
  @param:Schema(description = "Sequence of the responses", deprecated = true)
  val sequence: Int,
  @param:Schema(description = "Optional response as a date", nullable = true, example = "2024-04-29")
  val responseDate: LocalDate? = null,
  @param:Schema(description = "Optional additional information", nullable = true)
  val additionalInformation: String? = null,

  @param:Schema(description = "Username of person who responded to the question")
  val recordedBy: String,
  @param:Schema(description = "When the response was made", example = "2024-04-29T12:34:56.789012")
  val recordedAt: LocalDateTime,
)
