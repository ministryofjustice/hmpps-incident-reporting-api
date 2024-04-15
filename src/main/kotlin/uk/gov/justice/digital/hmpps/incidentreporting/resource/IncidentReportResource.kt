package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CreateIncidentReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.service.IncidentReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.IncidentReportService
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport as IncidentReportDTO

@RestController
@Validated
@RequestMapping("/incident-reports", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Incident Reports",
  description = "Returns incident report information",
)
class IncidentReportResource(
  private val incidentReportService: IncidentReportService,
) : EventBaseResource() {

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report information for this ID",
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
  fun getIncidentReport(
    @Schema(description = "The incident report Id", example = "de91dfa7-821f-4552-a427-bf2f32eafeb0", required = true)
    @PathVariable
    id: UUID,
  ): IncidentReportDTO {
    return incidentReportService.getIncidentReportById(id = id) ?: throw IncidentReportNotFoundException(id.toString())
  }

  @GetMapping("/incident-number/{incidentNumber}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the incident report information for this incident number",
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
  fun getIncidentReportByNumber(
    @Schema(description = "The incident report number", example = "2342341242", required = true)
    @PathVariable
    incidentNumber: String,
  ): IncidentReportDTO {
    return incidentReportService.getIncidentReportByIncidentNumber(incidentNumber) ?: throw IncidentReportNotFoundException(incidentNumber)
  }

  @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an incident report",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns created Incident Report",
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
      ApiResponse(
        responseCode = "409",
        description = "Incident Report already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createIncidentReport(
    @RequestBody
    @Validated
    incidentReportCreateRequest: CreateIncidentReportRequest,
  ): IncidentReportDTO {
    return eventPublishAndAudit(
      IncidentReportDomainEventType.INCIDENT_REPORT_CREATED,
      {
        incidentReportService.createIncidentReport(incidentReportCreateRequest)
      },
      InformationSource.DPS,
    )
  }
}
