package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description = "Request to add an addendum to the description of the incident report",
  accessMode = Schema.AccessMode.READ_ONLY,
)
@JsonInclude(JsonInclude.Include.ALWAYS)
class DescriptionAddendumRequest(
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
