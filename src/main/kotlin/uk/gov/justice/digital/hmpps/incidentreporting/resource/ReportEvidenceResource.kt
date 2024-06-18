package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Evidence
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddEvidence
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateEvidence
import java.util.UUID

@RestController
@Validated
class ReportEvidenceResource : ReportRelatedObjectsResource<Evidence, AddEvidence, UpdateEvidence>() {
  @GetMapping("/evidence")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns evidence for this incident report",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns evidence",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the VIEW_INCIDENT_REPORTS role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Data not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @Transactional(readOnly = true)
  override fun listObjects(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    reportId: UUID,
  ): List<Evidence> {
    return reportId.findReportOrThrowNotFound()
      .evidence.map { it.toDto() }
  }

  @PostMapping("/evidence")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds evidence to this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns evidence",
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
        description = "Missing required role. Requires the MAINTAIN_INCIDENT_REPORTS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Data not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @Transactional
  override fun addObject(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    reportId: UUID,
    @RequestBody
    @Valid
    request: AddEvidence,
  ): List<Evidence> {
    return reportId.updateReportOrThrowNotFound("Added evidence to incident report") { report ->
      with(request) {
        report.addEvidence(
          type = type,
          description = description,
        )
      }
      report.evidence.map { it.toDto() }
    }
  }

  @PatchMapping("/evidence/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update evidence in this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns evidence",
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
        description = "Missing required role. Requires the MAINTAIN_INCIDENT_REPORTS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Data not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @Transactional
  override fun updateObject(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    reportId: UUID,
    @Schema(description = "The index of the object to update (starts from 1)", example = "1", required = true)
    @PathVariable
    index: Int,
    @RequestBody
    @Valid
    request: UpdateEvidence,
  ): List<Evidence> {
    return reportId.updateReportOrThrowNotFound("Updated evidence in incident report") { report ->
      val objects = report.evidence
      objects.elementAtIndex(index).updateWith(request)
      objects.map { it.toDto() }
    }
  }

  @DeleteMapping("/evidence/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Remove evidence from this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns evidence",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the MAINTAIN_INCIDENT_REPORTS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Data not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @Transactional
  override fun removeObject(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    reportId: UUID,
    @Schema(description = "The index of the object to delete (starts from 1)", example = "1", required = true)
    @PathVariable
    index: Int,
  ): List<Evidence> {
    return reportId.updateReportOrThrowNotFound("Deleted evidence from incident report") { report ->
      val objects = report.evidence
      objects.removeElementAtIndex(index)
      objects.map { it.toDto() }
    }
  }
}
