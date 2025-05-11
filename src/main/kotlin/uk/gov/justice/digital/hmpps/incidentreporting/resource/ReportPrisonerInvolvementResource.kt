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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddPrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdatePrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.service.PrisonerInvolvementService
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.util.UUID

@RestController
@Validated
class ReportPrisonerInvolvementResource(
  private val relatedObjectService: PrisonerInvolvementService,
) : ReportRelatedObjectResource<PrisonerInvolvement, AddPrisonerInvolvement, UpdatePrisonerInvolvement>() {
  override val whatChanges = WhatChanged.PRISONERS_INVOLVED

  @GetMapping("/prisoners-involved")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns prisoner involvement for this incident report",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns prisoner involvement",
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
  ): List<PrisonerInvolvement> {
    return relatedObjectService.listObjects(reportId)
  }

  @PostMapping("/prisoners-involved")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds an involved prisoner to this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns prisoner involvement",
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
    request: AddPrisonerInvolvement,
  ): List<PrisonerInvolvement> {
    return publishChangeEvents("Added an involved prisoner to incident report") { now, requestUsername ->
      relatedObjectService.addObject(reportId, request, now, requestUsername)
    }
  }

  @PatchMapping("/prisoners-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update an involved prisoner in this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns prisoner involvement",
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
    request: UpdatePrisonerInvolvement,
  ): List<PrisonerInvolvement> {
    if (request.isEmpty) {
      return relatedObjectService.listObjects(reportId)
    }

    return publishChangeEvents("Updated an involved prisoner in incident report") { now, requestUsername ->
      relatedObjectService.updateObject(reportId, index, request, now, requestUsername)
    }
  }

  @DeleteMapping("/prisoners-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Remove an involved prisoner from this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns prisoner involvement",
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
  ): List<PrisonerInvolvement> {
    return publishChangeEvents("Deleted an involved prisoner from incident report") { now, requestUsername ->
      relatedObjectService.deleteObject(reportId, index, now, requestUsername)
    }
  }
}
