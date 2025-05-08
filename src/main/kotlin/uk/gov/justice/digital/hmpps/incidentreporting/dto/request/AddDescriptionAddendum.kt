package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(
  description = "Add to the description of an incident report by appending an addendum",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
class AddDescriptionAddendum(
  @Schema(
    description = "Username of user who added this addendum, defaulting to request token user",
    example = "USER_1",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 3,
    maxLength = 120,
  )
  @field:Size(min = 3, max = 120)
  val createdBy: String? = null,
  @Schema(
    description = "When addendum was added, defaulting to “now”",
    example = "2024-04-29T12:34:56.789012",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
  )
  val createdAt: LocalDateTime? = null,
  @Schema(
    description = "First name of person that added this addendum",
    example = "John",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 1,
    maxLength = 255,
  )
  @field:Size(min = 1, max = 255)
  val firstName: String,
  @Schema(
    description = "Last name of person that added this addendum",
    example = "Doe",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 1,
    maxLength = 255,
  )
  @field:Size(min = 1, max = 255)
  val lastName: String,
  @Schema(
    description = "Addendum text",
    example = "Internal investigation has concluded",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 1,
  )
  @field:Size(min = 1)
  val text: String,
)
