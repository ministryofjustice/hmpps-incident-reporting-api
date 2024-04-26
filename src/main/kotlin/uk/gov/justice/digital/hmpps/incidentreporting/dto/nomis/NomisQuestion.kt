package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisQuestion(
  @Schema(description = "The questionnaire question id")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incident")
  val sequence: Int,
  @Schema(description = "The Question being asked")
  val question: String,
  @Schema(description = "List of Responses to this question")
  val answers: List<NomisResponse> = listOf(),
)
