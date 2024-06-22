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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.StaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddStaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateStaffInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.util.UUID

@RestController
@Validated
class ReportStaffInvolvementResource : ReportRelatedObjectsResource<StaffInvolvement, AddStaffInvolvement, UpdateStaffInvolvement>() {
  @GetMapping("/staff-involved")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns staff involvement for this incident report",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns staff involvement",
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
  ): List<StaffInvolvement> {
    return reportId.findReportOrThrowNotFound()
      .staffInvolved.map { it.toDto() }
  }

  @PostMapping("/staff-involved")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds an involved member of staff to this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns staff involvement",
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
    request: AddStaffInvolvement,
  ): List<StaffInvolvement> {
    return reportId.updateReportOrThrowNotFound(
      "Added an involved member of staff to incident report",
      WhatChanged.STAFF_INVOLVED,
    ) { report ->
      with(request) {
        report.addStaffInvolved(
          staffUsername = staffUsername,
          staffRole = staffRole,
          comment = comment,
        )
      }
      report.staffInvolved.map { it.toDto() }
    }
  }

  @PatchMapping("/staff-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update an involved member of staff in this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns staff involvement",
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
    request: UpdateStaffInvolvement,
  ): List<StaffInvolvement> {
    return reportId.updateReportOrThrowNotFound(
      "Updated an involved member of staff in incident report",
      WhatChanged.STAFF_INVOLVED,
    ) { report ->
      val objects = report.staffInvolved
      objects.elementAtIndex(index).updateWith(request)
      objects.map { it.toDto() }
    }
  }

  @DeleteMapping("/staff-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Remove an involved member of staff from this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns staff involvement",
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
  ): List<StaffInvolvement> {
    return reportId.updateReportOrThrowNotFound(
      "Deleted an involved member of staff from incident report",
      WhatChanged.STAFF_INVOLVED,
    ) { report ->
      val objects = report.staffInvolved
      objects.removeElementAtIndex(index)
      objects.map { it.toDto() }
    }
  }
}
