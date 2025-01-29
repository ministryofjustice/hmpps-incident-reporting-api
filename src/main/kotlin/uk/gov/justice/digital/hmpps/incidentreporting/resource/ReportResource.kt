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
import jakarta.validation.constraints.Pattern
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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.ChangeStatusRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.ChangeTypeRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.SimplePage
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.toSimplePage
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportService
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
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
      description = "Filter by given human-readable report reference",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      type = "string",
      pattern = "^\\d+$",
      example = "\"11124143\"",
      minLength = 1,
      maxLength = 25,
    )
    @RequestParam(required = false)
    @Size(min = 1, max = 25)
    @Pattern(regexp = "^\\d+$")
    reference: String? = null,
    @Parameter(
      description = "Filter by given locations, typically prison IDs",
      example = "[LEI,MDI]",
      array = ArraySchema(
        schema = Schema(example = "MDI", minLength = 2, maxLength = 20),
        arraySchema = Schema(
          requiredMode = Schema.RequiredMode.NOT_REQUIRED,
          nullable = true,
          defaultValue = "null",
        ),
      ),
    )
    @RequestParam(required = false)
    location: List<
      @Size(min = 2, max = 20)
      String,
      >? = null,
    // TODO: `prisonId` can be removed once NOMIS reconciliation checks are updated to use `location`
    @Schema(
      description = "Filter by given location, typically a prison ID. Ignored if `location` is also used.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      deprecated = true,
      defaultValue = "null",
      example = "MDI",
      minLength = 2,
      maxLength = 6,
    )
    @RequestParam(required = false)
    @Size(min = 2, max = 6)
    prisonId: String? = null,
    @Schema(
      description = "Filter by given information source",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "DPS",
      implementation = InformationSource::class,
    )
    @RequestParam(required = false)
    source: InformationSource? = null,
    @Parameter(
      description = "Filter by given statuses",
      example = "[CLOSED,DUPLICATE]",
      array = ArraySchema(
        schema = Schema(implementation = Status::class),
        arraySchema = Schema(
          requiredMode = Schema.RequiredMode.NOT_REQUIRED,
          nullable = true,
          defaultValue = "null",
        ),
      ),
    )
    @RequestParam(required = false)
    status: List<Status>? = null,
    @Schema(
      description = "Filter by given incident type",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "DAMAGE",
      implementation = Type::class,
    )
    @RequestParam(required = false)
    type: Type? = null,
    @Schema(
      description = "Filter for incidents occurring since this date (inclusive)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "2024-01-01",
      format = "date",
    )
    @RequestParam(required = false)
    incidentDateFrom: LocalDate? = null,
    @Schema(
      description = "Filter for incidents occurring until this date (inclusive)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "2024-05-31",
      format = "date",
    )
    @RequestParam(required = false)
    incidentDateUntil: LocalDate? = null,
    @Schema(
      description = "Filter for incidents reported since this date (inclusive)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "2024-01-01",
      format = "date",
    )
    @RequestParam(required = false)
    reportedDateFrom: LocalDate? = null,
    @Schema(
      description = "Filter for incidents reported until this date (inclusive)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "2024-05-31",
      format = "date",
    )
    @RequestParam(required = false)
    reportedDateUntil: LocalDate? = null,
    @Schema(
      description = "Filter for incidents reported by username",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "abc12a",
    )
    @RequestParam(required = false)
    @Size(min = 3)
    reportedByUsername: String? = null,
    @Schema(
      description = "Filter for incidents involving staff identified by username",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "abc12a",
    )
    @RequestParam(required = false)
    @Size(min = 3)
    involvingStaffUsername: String? = null,
    @Schema(
      description = "Filter for incidents involving prisoners identified by prisoner number",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "A1234AA",
    )
    @RequestParam(required = false)
    @Size(min = 7, max = 10)
    involvingPrisonerNumber: String? = null,
    @ParameterObject
    @PageableDefault(page = 0, size = 20, sort = ["incidentDateAndTime"], direction = Sort.Direction.DESC)
    pageable: Pageable,
  ): SimplePage<ReportBasic> {
    if (pageable.pageSize > 50) {
      throw ValidationException("Page size must be 50 or less")
    }
    val locations = location ?: if (prisonId != null) listOf(prisonId) else emptyList()
    return reportService.getBasicReports(
      reference = reference,
      locations = locations,
      source = source,
      statuses = status ?: emptyList(),
      type = type,
      incidentDateFrom = incidentDateFrom,
      incidentDateUntil = incidentDateUntil,
      reportedDateFrom = reportedDateFrom,
      reportedDateUntil = reportedDateUntil,
      reportedByUsername = reportedByUsername,
      involvingStaffUsername = involvingStaffUsername,
      involvingPrisonerNumber = involvingPrisonerNumber,
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
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
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
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    id: UUID,
  ): ReportWithDetails {
    return reportService.getReportWithDetailsById(id = id)
      ?: throw ReportNotFoundException(id)
  }

  @GetMapping("/reference/{reportReference}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report (with only basic information) for this reference",
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
  fun getBasicReportByReference(
    @Schema(
      description = "The incident report reference",
      example = "\"11124143\"",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    reportReference: String,
  ): ReportBasic {
    return reportService.getBasicReportByReference(reportReference)
      ?: throw ReportNotFoundException(reportReference)
  }

  @GetMapping("/reference/{reportReference}/with-details")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report (with all related details) for this reference",
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
  fun getReportWithDetailsByReference(
    @Schema(
      description = "The incident report reference",
      example = "\"11124143\"",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    reportReference: String,
  ): ReportWithDetails {
    return reportService.getReportWithDetailsByReference(reportReference)
      ?: throw ReportNotFoundException(reportReference)
  }

  @PostMapping("")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a draft incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s creator.",
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
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
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
    @Schema(
      description = "The internal ID of the report to update",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    id: UUID,
    @RequestBody
    @Valid
    updateReportRequest: UpdateReportRequest,
  ): ReportBasic {
    if (updateReportRequest.isEmpty) {
      return reportService.getBasicReportById(id)
        ?: throw ReportNotFoundException(id)
    }

    return eventPublishAndAudit(
      ReportDomainEventType.INCIDENT_REPORT_AMENDED,
      InformationSource.DPS,
      WhatChanged.BASIC_REPORT,
    ) {
      reportService.updateReport(id, updateReportRequest)
        ?: throw ReportNotFoundException(id)
    }
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Changes the status of an existing incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
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
  fun changeReportStatus(
    @Schema(
      description = "The internal ID of the report to update",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    id: UUID,
    @RequestBody
    @Valid
    changeStatusRequest: ChangeStatusRequest,
  ): ReportWithDetails {
    val maybeChangedReport = reportService.changeReportStatus(id, changeStatusRequest)
      ?: throw ReportNotFoundException(id)

    return maybeChangedReport.alsoIfChanged {
      eventPublishAndAudit(
        ReportDomainEventType.INCIDENT_REPORT_AMENDED,
        InformationSource.DPS,
        WhatChanged.STATUS,
      ) {
        it
      }
    }.value
  }

  @PatchMapping("/{id}/type")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Changes the type of an existing incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. " +
      "Authentication token must provide a username which is recorded as the report’s modifier.",
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
  fun changeReportType(
    @Schema(
      description = "The internal ID of the report to update",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    id: UUID,
    @RequestBody
    @Valid
    changeTypeRequest: ChangeTypeRequest,
  ): ReportWithDetails {
    val maybeChangedReport = reportService.changeReportType(id, changeTypeRequest)
      ?: throw ReportNotFoundException(id)

    return maybeChangedReport.alsoIfChanged {
      eventPublishAndAudit(
        ReportDomainEventType.INCIDENT_REPORT_AMENDED,
        InformationSource.DPS,
        WhatChanged.TYPE,
      ) {
        it
      }
    }.value
  }

  // TODO: decide if a different role should be used!
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes an incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns deleted incident report",
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
  fun deleteReport(
    @Schema(
      description = "The incident report id",
      example = "11111111-2222-3333-4444-555555555555",
      requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @PathVariable
    id: UUID,
    @Schema(
      description = "Whether orphaned events should also be deleted",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
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
