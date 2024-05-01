package uk.gov.justice.digital.hmpps.incidentreporting.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Event linking multiple incident reports")
data class Event(
  @Schema(description = "The human-readable identifier of this report", required = true)
  val eventId: String,
  @Schema(description = "When the incident took place", required = true, example = "2024-04-29T12:34:56.789012")
  val eventDateAndTime: LocalDateTime,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = true, example = "MDI")
  val prisonId: String,

  @Schema(description = "Brief title describing the event", required = true)
  val title: String,
  @Schema(description = "Longer summary of the event", required = true)
  val description: String,

  @Schema(description = "When the report was first created", required = true, example = "2024-04-29T12:34:56.789012")
  val createdDate: LocalDateTime,
  @Schema(description = "When the report was last changed", required = true, example = "2024-04-29T12:34:56.789012")
  val lastModifiedDate: LocalDateTime,
  @Schema(description = "Username of the person who last changed this report", required = true)
  val lastModifiedBy: String,
) : Dto
