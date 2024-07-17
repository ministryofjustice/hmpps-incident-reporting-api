package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident report with only key information")
@JsonInclude(JsonInclude.Include.ALWAYS)
open class ReportBasic(
  @Schema(description = "The internal ID of this report", required = true)
  val id: UUID,
  @Schema(description = "The human-readable identifier of this report", required = true)
  val reportReference: String,
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

  @Schema(description = "Username of person who created the incident report", required = true)
  val reportedBy: String,
  @Schema(description = "When the incident report was created", required = true, example = "2024-04-29T12:34:56.789012")
  val reportedAt: LocalDateTime,
  @Schema(description = "The current status of this report", required = true, example = "DRAFT")
  val status: Status,
  @Schema(description = "Optional user who this report is currently assigned to", required = true, example = "null")
  val assignedTo: String?,

  @Schema(description = "When the report was first created", required = true)
  val createdAt: LocalDateTime,
  @Schema(description = "When the report was last changed", required = true)
  val modifiedAt: LocalDateTime,
  @Schema(description = "Username of the person who last changed this report", required = true)
  val modifiedBy: String,

  @Schema(description = "Whether the report was initially created in NOMIS as opposed to DPS", required = true, example = "false")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val createdInNomis: Boolean,
)
