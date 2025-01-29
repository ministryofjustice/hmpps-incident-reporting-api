package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident report with only key information", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
open class ReportBasic(
  @Schema(description = "The internal ID of this report")
  val id: UUID,
  @Schema(description = "The human-readable identifier of this report")
  val reportReference: String,
  @Schema(description = "Incident report type")
  val type: Type,
  @Schema(description = "When the incident took place", example = "2024-04-29T12:34:56.789012")
  val incidentDateAndTime: LocalDateTime,
  @Schema(description = "The location where incident took place, typically a NOMIS prison ID", example = "MDI")
  val location: String,

  @Schema(description = "Brief title describing the incident")
  val title: String,
  @Schema(description = "Longer summary of the incident")
  val description: String,

  @Schema(description = "Username of person who created the incident report")
  val reportedBy: String,
  @Schema(description = "When the incident report was created", example = "2024-04-29T12:34:56.789012")
  val reportedAt: LocalDateTime,
  @Schema(description = "The current status of this report", example = "DRAFT")
  val status: Status,
  @Schema(
    description = "Optional user who this report is currently assigned to (NB: this field will probably be removed)",
    example = "null",
    deprecated = true,
  )
  val assignedTo: String?,

  @Schema(description = "When the report was first created", example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @Schema(description = "When the report was last changed", example = "2024-04-29T12:34:56.789012")
  val modifiedAt: LocalDateTime,
  @Schema(description = "Username of the person who last changed this report")
  val modifiedBy: String,

  @Schema(description = "Whether the report was initially created in NOMIS as opposed to DPS", example = "false")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val createdInNomis: Boolean,
  @Schema(description = "Last modified in NOMIS as opposed to DPS", example = "false")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  val lastModifiedInNomis: Boolean,
) {
  // TODO: `prisonId` can be removed once NOMIS reconciliation checks are updated to use `location`
  @Suppress("unused")
  @get:Schema(
    description = "The location where incident took place, typically a NOMIS prison ID",
    deprecated = true,
    example = "MDI",
  )
  @get:JsonProperty
  val prisonId: String
    get() = location
}
