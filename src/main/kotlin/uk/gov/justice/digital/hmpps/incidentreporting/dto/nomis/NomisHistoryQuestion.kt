package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisHistoryQuestion(
  @Schema(description = "The sequence number of the response question for this incident")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incident")
  val sequence: Int,
  @Schema(description = "The Question being asked")
  val question: String,
  @Schema(description = "The question text as seen by the users in DPS", nullable = true)
  val questionLabel: String,
  @Schema(description = "Historical list of Responses to this question")
  val answers: List<NomisHistoryResponse> = listOf(),
)
