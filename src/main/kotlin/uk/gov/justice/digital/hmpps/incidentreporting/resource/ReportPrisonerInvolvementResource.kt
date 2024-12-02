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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddPrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdatePrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.util.UUID

@RestController
@Validated
class ReportPrisonerInvolvementResource() : ReportRelatedObjectsResource<PrisonerInvolvement, AddPrisonerInvolvement, UpdatePrisonerInvolvement>() {
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
  @Transactional(readOnly = true)
  override fun listObjects(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
  ): List<PrisonerInvolvement> {
    return reportId.findReportOrThrowNotFound()
      .prisonersInvolved.map { it.toDto() }
  }

  @PostMapping("/prisoners-involved")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds an involved prisoner to this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. Authentication token must provide a username which is recorded as the report’s modifier.",
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
  @Transactional
  override fun addObject(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
    @RequestBody
    @Valid
    request: AddPrisonerInvolvement,
  ): List<PrisonerInvolvement> {
    return reportId.updateReportOrThrowNotFound(
      "Added an involved prisoner to incident report",
      WhatChanged.PRISONERS_INVOLVED,
    ) { report ->
      with(request) {
        report.addPrisonerInvolved(
          prisonerNumber = prisonerNumber,
          prisonerRole = prisonerRole,
          outcome = outcome,
          comment = comment,
        )
      }
      report.prisonersInvolved.map { it.toDto() }
    }
  }

  @PatchMapping("/prisoners-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update an involved prisoner in this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. Authentication token must provide a username which is recorded as the report’s modifier.",
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
  @Transactional
  override fun updateObject(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
    @Schema(description = "The index of the object to update (starts from 1)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    index: Int,
    @RequestBody
    @Valid
    request: UpdatePrisonerInvolvement,
  ): List<PrisonerInvolvement> {
    if (request.isEmpty) {
      return reportId.findReportOrThrowNotFound().prisonersInvolved.map { it.toDto() }
    }

    return reportId.updateReportOrThrowNotFound(
      "Updated an involved prisoner in incident report",
      WhatChanged.PRISONERS_INVOLVED,
    ) { report ->
      report.findPrisonerInvolvedByIndex(index).updateWith(request)
      report.prisonersInvolved.map { it.toDto() }
    }
  }

  @DeleteMapping("/prisoners-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Remove an involved prisoner from this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. Authentication token must provide a username which is recorded as the report’s modifier.",
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
  @Transactional
  override fun removeObject(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
    @Schema(description = "The index of the object to delete (starts from 1)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    index: Int,
  ): List<PrisonerInvolvement> {
    return reportId.updateReportOrThrowNotFound(
      "Deleted an involved prisoner from incident report",
      WhatChanged.PRISONERS_INVOLVED,
    ) { report ->
      report.findPrisonerInvolvedByIndex(index).let { report.removePrisonerInvolved(it) }
      report.prisonersInvolved.map { it.toDto() }
    }
  }
}
