package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Incident report ID")
data class NomisSyncReportId(
  @Schema(description = "The internal ID of this report", required = true)
  val id: UUID,
)
