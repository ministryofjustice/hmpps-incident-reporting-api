package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status

@Schema(description = "Changes an incident report’s status")
data class ChangeStatusRequest(
  @Schema(description = "The new status", required = true, example = "CLOSED")
  val newStatus: Status,
)
