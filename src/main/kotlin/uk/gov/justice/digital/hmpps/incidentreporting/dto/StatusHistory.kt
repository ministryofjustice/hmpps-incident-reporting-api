package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import java.time.LocalDateTime

@Schema(description = "Previous statuses an incident report transitioned to")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StatusHistory(
  @Schema(description = "Previous current status of an incident report", required = true)
  val status: Status,
  @Schema(description = "When the report status was changed", required = true, example = "2024-04-29T12:34:56.789012")
  val setOn: LocalDateTime,
  @Schema(description = "The member of staff who changed the report status", required = true)
  val setBy: String,
) : Dto
