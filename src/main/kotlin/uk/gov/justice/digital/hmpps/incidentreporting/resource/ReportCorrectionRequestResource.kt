package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddCorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateCorrectionRequest
import uk.gov.justice.digital.hmpps.incidentreporting.service.CorrectionRequestService
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.util.UUID

@RestController
@Validated
class ReportCorrectionRequestResource(
  private val relatedObjectService: CorrectionRequestService,
) : ReportRelatedObjectResource<CorrectionRequest, AddCorrectionRequest, UpdateCorrectionRequest>() {
  override val whatChanges = WhatChanged.CORRECTION_REQUESTS

  @GetMapping("/correction-requests")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns correction requests for this incident report",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns correction requests",
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
  override fun listObjects(
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    reportId: UUID,
  ): List<CorrectionRequest> {
    return relatedObjectService.listObjects(reportId)
  }

  @PostMapping("/correction-requests")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds a correction request to this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the correction requester and report modifier.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns correction requests",
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
  override fun addObject(
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    reportId: UUID,
    @RequestBody
    @Valid
    request: AddCorrectionRequest,
  ): List<CorrectionRequest> {
    return publishChangeEvents("Added correction request to incident report") { now, requestUsername ->
      relatedObjectService.addObject(reportId, request, now, requestUsername)
    }
  }

  @PatchMapping("/correction-requests/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update a correction request in this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which overrides current correction requester and report modifier.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns correction requests",
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
  override fun updateObject(
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    reportId: UUID,
    @Schema(
      description = "The index of the object to update (starts from 1)",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    index: Int,
    @RequestBody
    @Valid
    request: UpdateCorrectionRequest,
  ): List<CorrectionRequest> {
    if (request.isEmpty) {
      return relatedObjectService.listObjects(reportId)
    }

    return publishChangeEvents("Updated a correction request in incident report") { now, requestUsername ->
      relatedObjectService.updateObject(reportId, index, request, now, requestUsername)
    }
  }

  @DeleteMapping("/correction-requests/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Remove a correction request from this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns correction requests",
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
  override fun removeObject(
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    reportId: UUID,
    @Schema(
      description = "The index of the object to delete (starts from 1)",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    index: Int,
  ): List<CorrectionRequest> {
    return publishChangeEvents("Deleted correction request from incident report") { now, requestUsername ->
      relatedObjectService.deleteObject(reportId, index, now, requestUsername)
    }
  }
}
