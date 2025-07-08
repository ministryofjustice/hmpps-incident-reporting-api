package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisHistoryQuestion(
  @param:Schema(description = "The sequence number of the response question for this incident")
  val questionId: Long,
  @param:Schema(description = "The sequence number of the question for this incident")
  val sequence: Int,
  @param:Schema(description = "The Question being asked")
  val question: String,
  @param:Schema(description = "Historical list of Responses to this question")
  val answers: List<NomisHistoryResponse> = emptyList(),
)
