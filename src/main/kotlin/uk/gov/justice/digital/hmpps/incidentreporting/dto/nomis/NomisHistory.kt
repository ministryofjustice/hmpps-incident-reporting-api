package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisHistory(
  @Schema(description = "The history questionnaire id for the incident")
  val questionnaireId: Long,
  @Schema(description = "The questionnaire type")
  val type: String,
  @Schema(description = "The questionnaire description")
  val description: String?,
  @Schema(description = "Questions asked for the questionnaire")
  val questions: List<NomisHistoryQuestion>,
  @Schema(description = "When the questionnaire was changed with time")
  val incidentChangeDateTime: LocalDateTime? = null,
  @Schema(description = "Who changed the questionnaire")
  val incidentChangeStaff: NomisStaff,

  @Schema(description = "The date and time the historical incident questionnaire was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the historical incident questionnaire")
  val createdBy: String,
)
