package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident report with only key information", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
open class ReportBasic(
  @param:Schema(description = "The internal ID of this report")
  val id: UUID,
  @param:Schema(description = "The human-readable identifier of this report")
  val reportReference: String,
  @param:Schema(description = "Incident report type")
  val type: Type,
  @param:Schema(description = "When the incident took place", example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime,
  @param:Schema(description = "The location where incident took place, typically a NOMIS prison ID", example = "MDI")
  val location: String,

  @param:Schema(description = "Brief title describing the incident")
  val title: String,
  @param:Schema(description = "Longer summary of the incident")
  val description: String,

  @param:Schema(description = "Username of person who created the incident report")
  val reportedBy: String,
  @param:Schema(description = "When the incident report was created", example = "2024-04-29T12:34:56.789012")
  val reportedAt: LocalDateTime,
  @param:Schema(description = "The current status of this report", example = "DRAFT")
  val status: Status,
  @param:Schema(description = "When the report was first created", example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @param:Schema(description = "When the report was last changed", example = "2024-04-29T12:34:56.789012")
  val modifiedAt: LocalDateTime,
  @param:Schema(description = "Username of the person who last changed this report")
  val modifiedBy: String,

  @param:Schema(description = "Whether the report was initially created in NOMIS as opposed to DPS", example = "false")
  @param:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val createdInNomis: Boolean,
  @param:Schema(description = "Last modified in NOMIS as opposed to DPS", example = "false")
  @param:JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val lastModifiedInNomis: Boolean,
  @param:Schema(
    description = "ID of the original report of which this report is a duplicate of",
    nullable = true,
  )
  val duplicatedReportId: UUID? = null,
  @param:Schema(description = "Latest user action from the most recent correction request", nullable = true)
  val latestUserAction: UserAction? = null,
)
