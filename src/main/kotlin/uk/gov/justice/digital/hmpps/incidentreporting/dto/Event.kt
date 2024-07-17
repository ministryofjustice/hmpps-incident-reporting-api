package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Event linking multiple incident reports")
@JsonInclude(JsonInclude.Include.ALWAYS)
data class Event(
  @Schema(description = "The human-readable identifier of this event", required = true)
  val eventReference: String,
  @Schema(description = "When the incident took place", required = true, example = "2024-04-29T12:34:56.789012")
  val eventDateAndTime: LocalDateTime,
  @Schema(description = "The NOMIS id of the prison where incident took place", required = true, example = "MDI")
  val prisonId: String,

  @Schema(description = "Brief title describing the event", required = true)
  val title: String,
  @Schema(description = "Longer summary of the event", required = true)
  val description: String,

  @Schema(description = "When the event was first created", required = true, example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @Schema(description = "When the event was last changed", required = true, example = "2024-04-29T12:34:56.789012")
  val modifiedAt: LocalDateTime,
  @Schema(description = "Username of the person who last changed this event", required = true)
  val modifiedBy: String,
)
