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

@Schema(description = "Payload to create a new draft incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class CreateReportRequest(
  @Schema(description = "Incident report type", requiredMode = Schema.RequiredMode.REQUIRED)
  val type: Type,
  @Schema(
    description = "When the incident took place",
    requiredMode = Schema.RequiredMode.REQUIRED,
    example = "2024-04-29T12:34:56.789012",
  )
  val incidentDateAndTime: LocalDateTime,
  @Schema(
    description = "The location where incident took place, typically a NOMIS prison ID",
    requiredMode = Schema.RequiredMode.REQUIRED,
    example = "MDI",
    minLength = 2,
    maxLength = 20,
  )
  @field:Size(min = 2, max = 20)
  val location: String,
  @Schema(
    description = "Brief title describing the incident",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 5,
    maxLength = 255,
  )
  @field:Size(min = 5, max = 255)
  val title: String,
  @Schema(description = "Longer summary of the incident", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
  @field:Size(min = 1)
  val description: String,
  @Schema(
    description = "Whether to link to a new event",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    defaultValue = "false",
  )
  val createNewEvent: Boolean = false,
  @Schema(
    description = "Which existing event to link to",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
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

  fun createReport(
    reportReference: String,
    event: Event,
    requestUsername: String,
    now: LocalDateTime,
  ): Report {
    val status = Status.DRAFT
    val report = Report(
      event = event,
      reportReference = reportReference,
      type = type,
      status = status,
      source = InformationSource.DPS,
      title = title,
      incidentDateAndTime = incidentDateAndTime,
      location = location,
      description = description,
      staffInvolvementDone = false,
      prisonerInvolvementDone = false,
      reportedBy = requestUsername,
      reportedAt = now,
      assignedTo = requestUsername,
      createdAt = now,
      modifiedAt = now,
      modifiedBy = requestUsername,
      modifiedIn = InformationSource.DPS,
    )
    report.addStatusHistory(status, now, requestUsername)
    return report
  }

  fun createEvent(
    generatedEventReference: String,
    requestUsername: String,
    now: LocalDateTime,
  ): Event {
    return Event(
      eventReference = generatedEventReference,
      eventDateAndTime = incidentDateAndTime,
      location = location,
      title = title,
      description = description,
      createdAt = now,
      modifiedAt = now,
      modifiedBy = requestUsername,
    )
  }
}
