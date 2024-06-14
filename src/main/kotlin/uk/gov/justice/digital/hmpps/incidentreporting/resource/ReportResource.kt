package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Size
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.SimplePage
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.toSimplePage
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportService
import java.time.LocalDate
import java.util.UUID

@RestController
@Validated
@RequestMapping("/incident-reports", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Incident reports",
  description = "Create, retrieve, update and delete incident reports",
)
class ReportResource(
  private val reportService: ReportService,
) : EventBaseResource() {
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns pages of filtered incident reports (with only basic information)",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a page of incident reports",
      ),
      ApiResponse(
        responseCode = "400",
        description = "When input parameters are not valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
    ],
  )
  fun getBasicReports(
    @Schema(
      description = "Filter by given prison ID",
      required = false,
      defaultValue = "null",
      example = "MDI",
      minLength = 2,
      maxLength = 10,
    )
    @RequestParam(required = false)
    @Size(min = 2, max = 6)
    prisonId: String? = null,
    @Schema(
      description = "Filter by given information source",
      required = false,
      defaultValue = "null",
      example = "DPS",
      implementation = InformationSource::class,
    )
    @RequestParam(required = false)
    source: InformationSource? = null,
    @Parameter(
      description = "Filter by given statuses",
      example = "CLOSED,DUPLICATE",
      array = ArraySchema(
        schema = Schema(implementation = Status::class),
        arraySchema = Schema(
          required = false,
          defaultValue = "null",
        ),
      ),
    )
    @RequestParam(required = false)
    status: List<Status>? = null,
    @Schema(
      description = "Filter by given incident type",
      required = false,
      defaultValue = "null",
      example = "DAMAGE",
      implementation = Type::class,
    )
    @RequestParam(required = false)
    type: Type? = null,
    @Schema(
      description = "Filter for incidents occurring since this date (inclusive)",
      required = false,
      defaultValue = "null",
      example = "2024-01-01",
      format = "date",
    )
    @RequestParam(required = false)
    incidentDateFrom: LocalDate? = null,
    @Schema(
      description = "Filter for incidents occurring until this date (inclusive)",
      required = false,
      defaultValue = "null",
      example = "2024-05-31",
      format = "date",
    )
    @RequestParam(required = false)
    incidentDateUntil: LocalDate? = null,
    @Schema(
      description = "Filter for incidents reported since this date (inclusive)",
      required = false,
      defaultValue = "null",
      example = "2024-01-01",
      format = "date",
    )
    @RequestParam(required = false)
    reportedDateFrom: LocalDate? = null,
    @Schema(
      description = "Filter for incidents reported until this date (inclusive)",
      required = false,
      defaultValue = "null",
      example = "2024-05-31",
      format = "date",
    )
    @RequestParam(required = false)
    reportedDateUntil: LocalDate? = null,
    @ParameterObject
    @PageableDefault(page = 0, size = 20, sort = ["incidentDateAndTime"], direction = Sort.Direction.DESC)
    pageable: Pageable,
  ): SimplePage<ReportBasic> {
    if (pageable.pageSize > 50) {
      throw ValidationException("Page size must be 50 or less")
    }
    return reportService.getBasicReports(
      prisonId = prisonId,
      source = source,
      statuses = status ?: emptyList(),
      type = type,
      incidentDateFrom = incidentDateFrom,
      incidentDateUntil = incidentDateUntil,
      reportedDateFrom = reportedDateFrom,
      reportedDateUntil = reportedDateUntil,
      pageable = pageable,
    )
      .toSimplePage()
  }

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report (with only basic information) for this ID",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns an incident report",
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
  fun getBasicReportById(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    id: UUID,
  ): ReportBasic {
    return reportService.getBasicReportById(id = id)
      ?: throw ReportNotFoundException(id)
  }

  @GetMapping("/{id}/with-details")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report (with all related details) for this ID",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns an incident report",
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
  fun getReportWithDetailsById(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    id: UUID,
  ): ReportWithDetails {
    return reportService.getReportWithDetailsById(id = id)
      ?: throw ReportNotFoundException(id)
  }

  @GetMapping("/incident-number/{incidentNumber}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report (with only basic information) for this incident number",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns an incident report",
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
  fun getBasicReportByIncidentNumber(
    @Schema(description = "The incident report number", example = "2342341242", required = true)
    @PathVariable
    incidentNumber: String,
  ): ReportBasic {
    return reportService.getBasicReportByIncidentNumber(incidentNumber)
      ?: throw ReportNotFoundException(incidentNumber)
  }

  @GetMapping("/incident-number/{incidentNumber}/with-details")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report (with all related details) for this incident number",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns an incident report",
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
  fun getReportWithDetailsByIncidentNumber(
    @Schema(description = "The incident report number", example = "2342341242", required = true)
    @PathVariable
    incidentNumber: String,
  ): ReportWithDetails {
    return reportService.getReportWithDetailsByIncidentNumber(incidentNumber)
      ?: throw ReportNotFoundException(incidentNumber)
  }

  @PostMapping("")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a draft incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns created draft incident report",
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
  fun createReport(
    @RequestBody
    @Valid
    createReportRequest: CreateReportRequest,
  ): ReportWithDetails {
    return eventPublishAndAudit(
      ReportDomainEventType.INCIDENT_REPORT_CREATED,
      InformationSource.DPS,
    ) {
      reportService.createReport(createReportRequest)
    }
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates key properties of an existing incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns updated incident report",
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
  fun updateReport(
    @Schema(description = "The internal ID of the report to update", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    id: UUID,
    @RequestBody
    @Valid
    updateReportRequest: UpdateReportRequest,
  ): ReportBasic {
    return eventPublishAndAudit(
      ReportDomainEventType.INCIDENT_REPORT_AMENDED,
      InformationSource.DPS,
    ) {
      reportService.updateReport(id, updateReportRequest)
        ?: throw ReportNotFoundException(id)
    }
  }

  // TODO: decide if a different role should be used!
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes an incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns deleted incident report",
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
  fun deleteReport(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", required = true)
    @PathVariable
    id: UUID,
    @Schema(
      description = "Whether orphaned events should also be deleted",
      required = false,
      defaultValue = "true",
      example = "false",
    )
    @RequestParam(required = false)
    deleteOrphanedEvents: Boolean = true,
  ): ReportWithDetails {
    return eventPublishAndAudit(
      ReportDomainEventType.INCIDENT_REPORT_DELETED,
      InformationSource.DPS,
    ) {
      reportService.deleteReportById(id, deleteOrphanedEvents)
        ?: throw ReportNotFoundException(id)
    }
  }
}
