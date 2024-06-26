package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime

@Schema(description = "Prior version of an incident report")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class History(
  @Schema(description = "Previous incident report type", required = true)
  val type: Type,
  @Schema(description = "When the report type was changed", required = true, example = "2024-04-29T12:34:56.789012")
  val changedAt: LocalDateTime,
  @Schema(description = "The member of staff who changed the report type", required = true)
  val changedBy: String,
  @Schema(description = "Previous set of question-response pairs", required = true)
  val questions: List<HistoricalQuestion> = emptyList(),
)
