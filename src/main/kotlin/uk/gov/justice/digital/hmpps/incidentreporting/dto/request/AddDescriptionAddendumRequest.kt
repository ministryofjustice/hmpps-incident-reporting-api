package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "Add to the description of an incident report by appending an addendum",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
class AddDescriptionAddendumRequest(
  @Schema(description = "Username of user who added this addendum", example = "USER_1")
  val createdBy: String,
  @Schema(description = "When addendum was added", example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @Schema(description = "First name of person that added this addendum", example = "John")
  val firstName: String,
  @Schema(description = "Last name of person that added this addendum", example = "Doe")
  val lastName: String,
  @Schema(description = "Addendum text", example = "Correction made to the report")
  val text: String,
)
