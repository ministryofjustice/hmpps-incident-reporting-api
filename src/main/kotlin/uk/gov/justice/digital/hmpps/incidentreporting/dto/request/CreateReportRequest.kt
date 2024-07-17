package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime

@Schema(description = "Payload to create a new draft incident report")
data class CreateReportRequest(
  @Schema(description = "Incident report type", required = true)
  val type: Type,
  @Schema(description = "When the incident took place", required = true, example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = true, example = "MDI", minLength = 2, maxLength = 6)
  @field:Size(min = 2, max = 6)
  val prisonId: String,
  @Schema(description = "Brief title describing the incident", required = true, minLength = 5, maxLength = 255)
  @field:Size(min = 5, max = 255)
  val title: String,
  @Schema(description = "Longer summary of the incident", required = true, minLength = 1)
  @field:Size(min = 1)
  val description: String,
  @Schema(description = "Whether to link to a new event", required = false, defaultValue = "false")
  val createNewEvent: Boolean = false,
  @Schema(description = "Which existing event to link to", required = false, defaultValue = "null")
  val linkedEventReference: String? = null,
) {
  fun validate(now: LocalDateTime) {
    if (!createNewEvent && linkedEventReference.isNullOrEmpty()) {
      throw ValidationException("Either createNewEvent or linkedEventReference must be provided")
    }
    if (!type.active) {
      throw ValidationException("Inactive incident type $type")
    }
    if (incidentDateAndTime > now) {
      throw ValidationException("incidentDateAndTime cannot be in the future")
    }
  }

  fun toNewEntity(incidentNumber: String, event: Event, requestUsername: String, now: LocalDateTime): Report {
    val status = Status.DRAFT
    val report = Report(
      incidentNumber = incidentNumber,
      type = type,
      title = title,
      incidentDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      description = description,
      reportedBy = requestUsername,
      reportedAt = now,
      status = status,
      createdAt = now,
      modifiedAt = now,
      modifiedBy = requestUsername,
      source = InformationSource.DPS,
      assignedTo = requestUsername,
      event = event,
    )
    report.addStatusHistory(status, now, requestUsername)
    return report
  }

  fun toNewEvent(generatedEventReference: String, requestUsername: String, now: LocalDateTime): Event {
    return Event(
      eventReference = generatedEventReference,
      eventDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      title = title,
      description = description,
      createdAt = now,
      modifiedAt = now,
      modifiedBy = requestUsername,
    )
  }
}
