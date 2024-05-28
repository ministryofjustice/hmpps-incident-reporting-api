package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import java.util.UUID

@Schema(description = "Incident report created/updated in NOMIS")
data class NomisSyncRequest(
  @Schema(
    description = "For updates, this value is the UUID of the existing incident. For new incidents, this value is null.",
    required = false,
    example = "123e4567-e89b-12d3-a456-426614174000",
  )
  val id: UUID? = null,
  @Schema(description = "For initial migration this is true", required = false, defaultValue = "false")
  val initialMigration: Boolean = false,
  @Schema(description = "Complete incident report payload", required = true)
  val incidentReport: NomisReport,
) {
  fun validate() {
    if (initialMigration && id != null) {
      throw ValidationException("Cannot update an existing report ($id) during initial migration")
    }
  }
}
