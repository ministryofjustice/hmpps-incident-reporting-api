package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncCreateRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncUpdateRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.NomisSyncReportId
import uk.gov.justice.digital.hmpps.incidentreporting.service.NomisSyncService
import io.swagger.v3.oas.annotations.parameters.RequestBody as RequestBodySchema

@RestController
@Validated
@RequestMapping("/sync", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Migrate/Upsert NOMIS Incident Report",
  description = "Migrate or synchronise NOMIS incident report to Incident Report Service",
)
@PreAuthorize("hasRole('ROLE_MIGRATE_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
class NomisSyncResource(
  private val syncService: NomisSyncService,
) : EventBaseResource() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/upsert")
  @Operation(
    summary = "Migrate a report",
    description = "Requires role MIGRATE_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Migrated NOMIS incident report id",
      ),
      ApiResponse(
        responseCode = "200",
        description = "Updated NOMIS incident report id",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
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
    @RequestBodySchema(
      description = "Incident report created or updated in NOMIS",
      content = [
        Content(
          schema = Schema(
            accessMode = Schema.AccessMode.WRITE_ONLY,
            oneOf = [NomisSyncRequest::class, NomisSyncCreateRequest::class, NomisSyncUpdateRequest::class],
          ),
        ),
      ],
    )
    @RequestBody
    @Valid
    syncRequest: NomisSyncRequest,
  ): ResponseEntity<NomisSyncReportId> {
    val isUpdate = syncRequest.id != null
    val report = syncService.upsert(syncRequest)
    val status = if (isUpdate) {
      HttpStatus.OK
    } else {
      HttpStatus.CREATED
    }
    log.info("Incident report synchronised: ${report.reportReference}")
    return ResponseEntity(NomisSyncReportId(report.id), status)
  }
}
