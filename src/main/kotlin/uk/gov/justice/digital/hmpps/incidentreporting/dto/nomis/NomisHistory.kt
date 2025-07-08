package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisHistory(
  @param:Schema(description = "The history questionnaire id for the incident")
  val questionnaireId: Long,
  @param:Schema(description = "The questionnaire type")
  val type: String,
  @param:Schema(description = "The questionnaire description")
  val description: String?,
  @param:Schema(description = "Questions asked for the questionnaire")
  val questions: List<NomisHistoryQuestion>,
  @param:Schema(description = "When the questionnaire was changed with time")
  val incidentChangeDateTime: LocalDateTime? = null,
  @param:Schema(description = "Who changed the questionnaire")
  val incidentChangeStaff: NomisStaff,

  @param:Schema(description = "The date and time the historical incident questionnaire was created")
  val createDateTime: LocalDateTime,
  @param:Schema(description = "The username of the person who created the historical incident questionnaire")
  val createdBy: String,
)
