package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import java.util.UUID

@Schema(description = "IncidentReport Details raised/updated in NOMIS")
data class NomisSyncRequest(
  @Schema(
    description = "For updates, this value is the UUID of the existing incident. For new incidents, this value is null.",
    required = false,
    example = "123e4567-e89b-12d3-a456-426614174000",
  )
  val id: UUID? = null,
  @Schema(description = "For initial migration this is true", required = false, defaultValue = "false")
  val initialMigration: Boolean = false,
  @Schema(description = "IncidentReport Details raised/updated in NOMIS", required = true)
  val incidentReport: NomisReport,
)
