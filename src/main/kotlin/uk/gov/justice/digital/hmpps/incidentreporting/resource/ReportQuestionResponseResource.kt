package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Question
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.AddOrUpdateQuestionWithResponses
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportService
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import java.util.UUID

@RestController
@Validated
@RequestMapping("/incident-reports/{reportId}/questions", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Questions and responses forming an incident report",
  description = "Create, retrieve and delete question-response pairs that form incident reports",
)
class ReportQuestionResponseResource(
  private val reportService: ReportService,
) : EventBaseResource() {
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns all non-historic questions in an incident report with their responses",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns all questions and responses in report",
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
  fun getQuestionsAndResponses(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
  ): List<Question> {
    return reportService.getQuestionsWithResponses(reportId)
      ?: throw ReportNotFoundException(reportId)
  }

  @PutMapping("")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Add new questions with responses to the end of the current list or update them in place if question codes match",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. Authentication token must provide a username which is recorded as the report’s modifier.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns all questions and responses in report",
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
  fun addOrUpdateQuestionsWithResponses(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
    @Parameter(
      description = "List of question and responses to add or update in place",
      array = ArraySchema(
        schema = Schema(implementation = AddOrUpdateQuestionWithResponses::class),
        arraySchema = Schema(
          requiredMode = Schema.RequiredMode.REQUIRED,
          nullable = false,
        ),
        minItems = 1,
      ),
    )
    @RequestBody
    @Size(min = 1)
    @Valid
    requests: List<AddOrUpdateQuestionWithResponses>,
  ): List<Question> {
    val (report, questions) = reportService.addOrUpdateQuestionsWithResponses(reportId, requests)
      ?: throw ReportNotFoundException(reportId)
    eventPublishAndAudit(
      ReportDomainEventType.INCIDENT_REPORT_AMENDED,
      InformationSource.DPS,
      WhatChanged.QUESTIONS,
    ) {
      report
    }
    return questions
  }

  @DeleteMapping("")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_INCIDENT_REPORTS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes questions (identified by code) from an incident report along with their responses",
    description = "Requires role MAINTAIN_INCIDENT_REPORTS and write scope. Authentication token must provide a username which is recorded as the report’s modifier.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns all remaining questions and responses in report",
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
  fun deleteQuestionsAndResponses(
    @Schema(description = "The incident report id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    reportId: UUID,
    @Parameter(
      description = "Codes for questions to delete",
      example = "[40102,40103]",
      array = ArraySchema(
        schema = Schema(example = "40102", minLength = 1),
        arraySchema = Schema(
          requiredMode = Schema.RequiredMode.REQUIRED,
          nullable = false,
          minLength = 1,
        ),
      ),
    )
    @RequestParam(required = false)
    @Size(min = 1)
    code: Set<
      @Size(min = 1)
      String,
      > = emptySet(),
  ): List<Question> {
    val (report, questions) = reportService.deleteQuestionsAndResponses(reportId, code)
      ?: throw ReportNotFoundException(reportId)
    eventPublishAndAudit(
      ReportDomainEventType.INCIDENT_REPORT_AMENDED,
      InformationSource.DPS,
      WhatChanged.QUESTIONS,
    ) {
      report
    }
    return questions
  }
}
