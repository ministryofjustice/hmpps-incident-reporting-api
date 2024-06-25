package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime

@Schema(description = "Payload to update key properties of an incident report")
data class UpdateReportRequest(
  @Schema(description = "When the incident took place", required = false, defaultValue = "null", example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime? = null,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = false, defaultValue = "null", example = "MDI")
  @field:Size(min = 2, max = 6)
  val prisonId: String? = null,
  @Schema(description = "Brief title describing the incident", required = false, defaultValue = "null")
  @field:Size(min = 5, max = 255)
  val title: String? = null,
  @Schema(description = "Longer summary of the incident", required = false, defaultValue = "null")
  @field:Size(min = 1)
  val description: String? = null,

  @Schema(description = "Whether the parent event should also be updated", required = false, defaultValue = "false", example = "true")
  val updateEvent: Boolean = false,
) {
  fun validate(now: LocalDateTime) {
    if (incidentDateAndTime != null && incidentDateAndTime > now) {
      throw ValidationException("incidentDateAndTime cannot be in the future")
    }
  }

  fun updateExistingReport(report: Report, requestUsername: String, now: LocalDateTime): Report {
    incidentDateAndTime?.let { report.incidentDateAndTime = it }
    prisonId?.let { report.prisonId = it }
    title?.let { report.title = it }
    description?.let { report.description = it }
    report.modifiedBy = requestUsername
    report.modifiedAt = now

    if (updateEvent) {
      val event = report.event
      incidentDateAndTime?.let { event.eventDateAndTime = it }
      prisonId?.let { event.prisonId = it }
      title?.let { event.title = it }
      description?.let { event.description = it }
      event.modifiedBy = requestUsername
      event.modifiedAt = now
    }

    return report
  }
}
