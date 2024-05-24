package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.incidentreporting.config.ValidatableWithMethod
import uk.gov.justice.digital.hmpps.incidentreporting.config.ValidateWithMethod
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Event
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.Clock
import java.time.LocalDateTime

@Schema(description = "Payload to create a new incident report")
@ValidateWithMethod(message = "Invalid create report request ({error})")
data class CreateReportRequest(
  @Schema(description = "Incident report type", required = true)
  val type: Type,
  @Schema(description = "When the incident took place", required = true, example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = true, example = "MDI")
  val prisonId: String,
  @Schema(description = "Brief title describing the incident", required = true)
  val title: String,
  @Schema(description = "Longer summary of the incident", required = true)
  val description: String,
  @Schema(description = "Whether to link to a new event", required = false, defaultValue = "false")
  val createNewEvent: Boolean = false,
  @Schema(description = "Which existing event to link to", required = false, defaultValue = "null")
  val linkedEventId: String? = null,
  @Schema(description = "Username of person who created the incident report", required = true)
  val reportedBy: String,
  @Schema(description = "When the incident report was created", required = true, example = "2024-04-29T12:34:56.789012")
  val reportedDate: LocalDateTime,
  // TODO: there is not yet a way to add any more details to a report, question-response pairs, etc
) : ValidatableWithMethod {
  override fun validate() {
    if (!createNewEvent && linkedEventId.isNullOrEmpty()) {
      throw ValidationException("Either createNewEvent or linkedEventId must be provided")
    }
    if (!type.active) {
      throw ValidationException("Inactive incident type $type")
    }
  }

  fun toNewEntity(incidentNumber: String, event: Event, createdBy: String, clock: Clock): Report {
    val now = LocalDateTime.now(clock)
    val status = Status.DRAFT
    val report = Report(
      incidentNumber = incidentNumber,
      type = type,
      title = title,
      incidentDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      description = description,
      reportedBy = reportedBy,
      reportedAt = reportedDate,
      status = status,
      createdAt = now,
      modifiedAt = now,
      modifiedBy = createdBy,
      source = InformationSource.DPS,
      assignedTo = reportedBy,
      event = event,
    )
    report.addStatusHistory(status, now, createdBy)
    return report
  }

  fun toNewEvent(generateEventId: String, createdBy: String, clock: Clock): Event {
    val now = LocalDateTime.now(clock)
    return Event(
      eventId = generateEventId,
      eventDateAndTime = incidentDateAndTime,
      prisonId = prisonId,
      title = title,
      description = description,
      createdAt = now,
      modifiedAt = now,
      modifiedBy = createdBy,
    )
  }
}
