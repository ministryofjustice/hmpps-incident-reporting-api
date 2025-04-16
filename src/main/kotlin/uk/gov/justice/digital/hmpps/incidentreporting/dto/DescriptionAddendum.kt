package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Addendum to the description", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class DescriptionAddendum(
  @Schema(description = "Username of user who added this addendum", example = "USER_1")
  val createdBy: String,
  @Schema(description = "When addendum was added", example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @Schema(description = "Addendum text")
  val text: String,
)
