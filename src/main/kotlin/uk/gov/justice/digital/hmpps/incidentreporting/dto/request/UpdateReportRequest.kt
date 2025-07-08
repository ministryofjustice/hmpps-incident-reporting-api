package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.time.LocalDateTime

@Schema(
  description = "Payload to update key properties of an incident report",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class UpdateReportRequest(
  @param:Schema(
    description = "When the incident took place",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    example = "2024-04-29T12:34:56.789012",
  )
  val incidentDateAndTime: LocalDateTime? = null,
  @param:Schema(
    description = "The location where incident took place, typically a NOMIS prison ID",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    example = "MDI",
    minLength = 2,
    maxLength = 20,
  )
  @field:Size(min = 2, max = 20)
  val location: String? = null,
  @param:Schema(
    description = "Brief title describing the incident",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 5,
    maxLength = 255,
  )
  @field:Size(min = 5, max = 255)
  val title: String? = null,
  @param:Schema(
    description = "Longer summary of the incident",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 1,
  )
  @field:Size(min = 1)
  val description: String? = null,

  @param:Schema(
    description = "Internal flag which, " +
      "if false, is used to indicate that addition of staff involvements is unfinished",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val staffInvolvementDone: Boolean? = null,
  @param:Schema(
    description = "Internal flag which, " +
      "if false, is used to indicate that addition of prisoner involvements is unfinished",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val prisonerInvolvementDone: Boolean? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    incidentDateAndTime == null &&
      location == null &&
      title == null &&
      description == null &&
      staffInvolvementDone == null &&
      prisonerInvolvementDone == null

  fun validate(now: LocalDateTime) {
    if (incidentDateAndTime != null && incidentDateAndTime > now) {
      throw ValidationException("incidentDateAndTime cannot be in the future")
    }
  }

  fun updateExistingReport(
    report: Report,
    requestUsername: String,
    now: LocalDateTime,
  ): Report {
    incidentDateAndTime?.let { report.incidentDateAndTime = it }
    location?.let { report.location = it }
    title?.let { report.title = it }
    description?.let { report.description = it }
    staffInvolvementDone?.let { report.staffInvolvementDone = it }
    prisonerInvolvementDone?.let { report.prisonerInvolvementDone = it }

    report.modifiedIn = InformationSource.DPS
    report.modifiedBy = requestUsername
    report.modifiedAt = now

    return report
  }
}
