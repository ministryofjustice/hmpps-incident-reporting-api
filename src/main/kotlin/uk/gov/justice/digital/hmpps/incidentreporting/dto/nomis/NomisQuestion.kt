package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisQuestion(
  @param:Schema(description = "The questionnaire question id")
  val questionId: Long,
  @param:Schema(description = "The sequence number of the question for this incident")
  val sequence: Int,

  @param:Schema(description = "The date and time the question was created")
  val createDateTime: LocalDateTime,
  @param:Schema(description = "The username of the person who created the question")
  val createdBy: String,

  @param:Schema(description = "The Question being asked")
  val question: String,

  @param:Schema(description = "List of Responses to this question")
  val answers: List<NomisResponse> = emptyList(),
)
