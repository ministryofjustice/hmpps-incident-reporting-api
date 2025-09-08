package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime

@Schema(description = "Previous incident type of an incident report", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class IncidentTypeHistory(
  @param:Schema(description = "Previous incident report type")
  val type: Type,
  @param:Schema(description = "When the report type was changed", example = "2024-04-29T12:34:56.789012")
  val changedAt: LocalDateTime,
  @param:Schema(description = "The member of staff who changed the report type")
  val changedBy: String,
)
