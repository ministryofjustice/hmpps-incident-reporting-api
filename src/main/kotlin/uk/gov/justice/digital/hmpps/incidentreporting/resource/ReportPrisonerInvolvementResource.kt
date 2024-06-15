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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddPrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import java.util.UUID

@RestController
@Validated
class ReportPrisonerInvolvementResource : ReportRelatedObjectsResource<PrisonerInvolvement, AddPrisonerInvolvement>() {
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
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
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
    summary = "Adds an invovled prisoner to this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
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
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    reportId: UUID,
    @RequestBody
    @Valid
    request: AddPrisonerInvolvement,
  ): List<PrisonerInvolvement> {
    val report = reportId.findReportOrThrowNotFound()
    with(request) {
      report.addPrisonerInvolved(
        prisonerNumber = prisonerNumber,
        prisonerRole = prisonerRole,
        outcome = outcome,
        comment = comment,
      )
    }
    eventPublishAndAudit(
      // TODO: should this be more specific?
      ReportDomainEventType.INCIDENT_REPORT_AMENDED,
      InformationSource.DPS,
    ) {
      report.toDtoBasic()
    }
    return report.prisonersInvolved.map { it.toDto() }
  }

  @DeleteMapping("/prisoners-involved/{index}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Remove an involved prisoner from this incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
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
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    reportId: UUID,
    @Schema(description = "The index of the object to delete (starts from 1)", example = "1", required = true)
    @PathVariable
    index: Int,
  ): List<PrisonerInvolvement> {
    val report = reportId.findReportOrThrowNotFound()
    val objects = report.prisonersInvolved
    if (index < 1 || index > objects.size) {
      throw ObjectAtIndexNotFoundException(PrisonerInvolvement::class, index)
    }
    objects.removeAt(index - 1)
    eventPublishAndAudit(
      // TODO: should this be more specific?
      ReportDomainEventType.INCIDENT_REPORT_AMENDED,
      InformationSource.DPS,
    ) {
      report.toDtoBasic()
    }
    return objects.map { it.toDto() }
  }
}
