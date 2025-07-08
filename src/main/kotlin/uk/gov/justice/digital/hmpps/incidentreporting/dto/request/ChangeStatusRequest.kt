package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status

@Schema(description = "Changes an incident report’s status", accessMode = Schema.AccessMode.WRITE_ONLY)
data class ChangeStatusRequest(
  @param:Schema(description = "The new status", requiredMode = Schema.RequiredMode.REQUIRED, example = "CLOSED")
  val newStatus: Status,
)
