package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.Optional

@Schema(
  description = "Update a description addendum in an incident report",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
class UpdateDescriptionAddendum(
  @Schema(
    description = "Username of user who added this addendum",
    example = "USER_1",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 3,
    maxLength = 120,
  )
  @field:Size(min = 3, max = 120)
  val createdBy: String? = null,
  @Schema(
    description = "When addendum was added " +
      "– omit to preserve existing date, provide null to set it to “now”",
    example = "2024-04-29T12:34:56.789012",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
  )
  val createdAt: Optional<LocalDateTime>? = null,
  @Schema(
    description = "First name of person that added this addendum",
    example = "John",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 1,
    maxLength = 255,
  )
  @field:Size(min = 1, max = 255)
  val firstName: String? = null,
  @Schema(
    description = "Last name of person that added this addendum",
    example = "Doe",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 1,
    maxLength = 255,
  )
  @field:Size(min = 1, max = 255)
  val lastName: String? = null,
  @Schema(
    description = "Addendum text",
    example = "Correction made to the report",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    minLength = 1,
  )
  @field:Size(min = 1)
  val text: String? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    createdBy == null &&
      createdAt == null &&
      firstName == null &&
      lastName == null &&
      text == null
}
