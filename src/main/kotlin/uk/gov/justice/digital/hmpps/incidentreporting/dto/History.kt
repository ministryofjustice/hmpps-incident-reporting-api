package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime

@Schema(description = "Prior version of an incident report", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class History(
  @Schema(description = "Previous incident report type")
  val type: Type,
  @Schema(description = "When the report type was changed", example = "2024-04-29T12:34:56.789012")
  val changedAt: LocalDateTime,
  @Schema(description = "The member of staff who changed the report type")
  val changedBy: String,
  @Schema(description = "Previous set of question-response pairs")
  val questions: List<HistoricalQuestion> = emptyList(),
) {
  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @Suppress("unused")
  @get:Schema(description = "Previous NOMIS incident report type code, which may be null for newer incident types", nullable = true, deprecated = true)
  @get:JsonProperty
  val nomisType: String?
    get() = type.nomisType
}
