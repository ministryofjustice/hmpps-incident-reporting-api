package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.Clock
import java.time.LocalDateTime

@Schema(description = "Payload to update key properties of an incident report")
data class UpdateReportRequest(
  @Schema(description = "When the incident took place", required = false, defaultValue = "null", example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime? = null,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = false, defaultValue = "null", example = "MDI")
  @field:Size(min = 2, max = 6)
  val prisonId: String? = null,
  @Schema(description = "Brief title describing the incident", required = false, defaultValue = "null")
  @field:Size(min = 10, max = 255)
  val title: String? = null,
  @Schema(description = "Longer summary of the incident", required = false, defaultValue = "null")
  val description: String? = null,
  @Schema(description = "Username of person who created the incident report", required = false, defaultValue = "null")
  @field:Size(min = 3, max = 120)
  val reportedBy: String? = null,
  @Schema(description = "When the incident report was created", required = false, defaultValue = "null", example = "2024-04-29T12:34:56.789012")
  val reportedAt: LocalDateTime? = null,
) {
  fun validate() {
    if (incidentDateAndTime != null && reportedAt != null && reportedAt < incidentDateAndTime) {
      throw ValidationException("incidentDateAndTime must be before reportedAt")
    }
  }

  fun updateExistingReport(report: Report, updatedBy: String, clock: Clock): Report {
    val now = LocalDateTime.now(clock)
    incidentDateAndTime?.let { report.incidentDateAndTime = it }
    prisonId?.let { report.prisonId = it }
    title?.let { report.title = it }
    description?.let { report.description = it }
    reportedBy?.let { report.reportedBy = it }
    reportedAt?.let { report.reportedAt = it }
    report.modifiedBy = updatedBy
    report.modifiedAt = now
    return report
  }
}
