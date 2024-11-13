package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.dto.EventWithBasicReports
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.SimplePage
import uk.gov.justice.digital.hmpps.incidentreporting.dto.response.toSimplePage
import uk.gov.justice.digital.hmpps.incidentreporting.service.EventService
import java.time.LocalDate
import java.util.UUID

@RestController
@Validated
@RequestMapping("/incident-events", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Incident events",
  description = "Retrieve events. These are groups of reports related to one incident",
)
class EventResource(
  private val eventService: EventService,
) {
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns pages of events (without contained reports)",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a page of events",
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
  fun getEvents(
    @Parameter(
      description = "Filter by given locations, typically prison IDs",
      example = "LEI,MDI",
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
    @Schema(
      description = "Filter for events that happened since this date (inclusive)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "2024-01-01",
      format = "date",
    )
    @RequestParam(required = false)
    eventDateFrom: LocalDate? = null,
    @Schema(
      description = "Filter for events that happened until this date (inclusive)",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      defaultValue = "null",
      example = "2024-05-31",
      format = "date",
    )
    @RequestParam(required = false)
    eventDateUntil: LocalDate? = null,
    @ParameterObject
    @PageableDefault(page = 0, size = 20, sort = ["eventDateAndTime"], direction = Sort.Direction.DESC)
    pageable: Pageable,
  ): SimplePage<EventWithBasicReports> {
    if (pageable.pageSize > 50) {
      throw ValidationException("Page size must be 50 or less")
    }
    return eventService.getEvents(
      locations = location ?: emptyList(),
      eventDateFrom = eventDateFrom,
      eventDateUntil = eventDateUntil,
      pageable = pageable,
    )
      .toSimplePage()
  }

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the event (without contained reports) for this ID",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns an event",
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
  fun getEventById(
    @Schema(description = "The event id", example = "11111111-2222-3333-4444-555555555555", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    id: UUID,
  ): EventWithBasicReports {
    return eventService.getEventById(id)
      ?: throw EventNotFoundException(id)
  }

  @GetMapping("/reference/{eventReference}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasRole('ROLE_VIEW_INCIDENT_REPORTS')")
  @Operation(
    summary = "Returns the event (without contained reports) for this reference",
    description = "Requires role VIEW_INCIDENT_REPORTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns an event",
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
  fun getEventByReference(
    @Schema(description = "The event reference", example = "2342341242", requiredMode = Schema.RequiredMode.REQUIRED)
    @PathVariable
    eventReference: String,
  ): EventWithBasicReports {
    return eventService.getEventByReference(eventReference)
      ?: throw EventNotFoundException(eventReference)
  }
}
