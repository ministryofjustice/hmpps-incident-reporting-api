package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Event linking multiple incident reports", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
open class Event(
  @Schema(description = "The internal ID of this event")
  val id: UUID,
  @Schema(description = "The human-readable identifier of this event")
  val eventReference: String,
  @Schema(description = "When the incident took place", example = "2024-04-29T12:34:56.789012")
  val eventDateAndTime: LocalDateTime,
  @Schema(description = "The location where incident took place, typically a NOMIS prison ID", example = "MDI")
  val location: String,

  @Schema(description = "Brief title describing the event")
  val title: String,
  @Schema(description = "Longer summary of the event")
  val description: String,

  @Schema(description = "When the event was first created", example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @Schema(description = "When the event was last changed", example = "2024-04-29T12:34:56.789012")
  val modifiedAt: LocalDateTime,
  @Schema(description = "Username of the person who last changed this event")
  val modifiedBy: String,
) {
  // TODO: `prisonId` can be removed once NOMIS reconciliation checks are updated to use `location`
  @get:Schema(description = "The location where incident took place, typically a NOMIS prison ID", deprecated = true, example = "MDI")
  @get:JsonProperty
  val prisonId: String
    get() = location
}
