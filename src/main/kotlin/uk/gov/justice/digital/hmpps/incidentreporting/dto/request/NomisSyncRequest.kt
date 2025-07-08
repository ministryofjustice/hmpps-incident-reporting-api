package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import java.util.UUID

@Schema(description = "Incident report created or updated in NOMIS", accessMode = Schema.AccessMode.WRITE_ONLY)
data class NomisSyncRequest(
  @get:Schema(
    description = "Incident report ID, required for updates and must be omitted for initial migration",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    example = "123e4567-e89b-12d3-a456-426614174000",
    nullable = true,
  )
  val id: UUID? = null,
  @get:Schema(
    description = "Set to true for initial migration, omit or set to false for updates",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    example = "true",
    defaultValue = "false",
  )
  val initialMigration: Boolean = false,
  @get:Schema(description = "Complete incident report payload", requiredMode = Schema.RequiredMode.REQUIRED)
  val incidentReport: NomisReport,
) {
  fun validate() {
    if (initialMigration && id != null) {
      throw ValidationException("Cannot update an existing report ($id) during initial migration")
    }
  }
}

@Schema(description = "Incident report created in NOMIS", accessMode = Schema.AccessMode.WRITE_ONLY)
interface NomisSyncCreateRequest {
  @Suppress("unused")
  @get:Schema(
    description = "Set to true for initial migration",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    defaultValue = "false",
    example = "true",
  )
  val initialMigration: Boolean

  @Suppress("unused")
  @get:Schema(description = "Complete incident report payload", requiredMode = Schema.RequiredMode.REQUIRED)
  val incidentReport: NomisReport
}

@Schema(description = "Incident report updated in NOMIS", accessMode = Schema.AccessMode.WRITE_ONLY)
interface NomisSyncUpdateRequest {
  @get:Schema(
    description = "Incident report ID",
    requiredMode = Schema.RequiredMode.REQUIRED,
    example = "123e4567-e89b-12d3-a456-426614174000",
  )
  val id: UUID

  @Suppress("unused")
  @get:Schema(
    description = "Omit or set to false for updates",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    defaultValue = "false",
  )
  val initialMigration: Boolean

  @Suppress("unused")
  @get:Schema(description = "Complete incident report payload", requiredMode = Schema.RequiredMode.REQUIRED)
  val incidentReport: NomisReport
}
