package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.SyncService
import java.util.UUID

@RestController
@Validated
@RequestMapping("/sync", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Migrate/Upsert NOMIS Incident Report",
  description = "Migrate or upsert NOMIS incident Report to IncidentReport Service.",
)
@PreAuthorize("hasRole('ROLE_MIGRATE_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
class NomisSyncResource(
  private val syncService: SyncService,
) : EventBaseResource() {

  @PostMapping("/upsert")
  @Operation(
    summary = "Migrate a location",
    description = "Requires role MIGRATE_LOCATIONS and write scope",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Migrated NOMIS Incident Report",
      ),
      ApiResponse(
        responseCode = "201",
        description = "Updated NOMIS Incident Report",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the MIGRATE_INCIDENT_REPORTS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Data not found, when id is provided",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun upsertIncidentReport(
    @RequestBody
    @Validated
    syncRequest: NomisSyncRequest,
  ): ResponseEntity<uk.gov.justice.digital.hmpps.incidentreporting.dto.Report> {
    val result = syncService.upsert(syncRequest)
    return ResponseEntity(
      if (syncRequest.initialMigration) {
        result
      } else {
        val eventType = if (syncRequest.id != null) {
          ReportDomainEventType.INCIDENT_REPORT_AMENDED
        } else {
          ReportDomainEventType.INCIDENT_REPORT_CREATED
        }
        eventPublishAndAudit(
          eventType,
          function = {
            result
          },
          informationSource = InformationSource.NOMIS,
        )
      },
      if (syncRequest.id != null) {
        HttpStatus.OK
      } else {
        HttpStatus.CREATED
      },

    )
  }
}

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
