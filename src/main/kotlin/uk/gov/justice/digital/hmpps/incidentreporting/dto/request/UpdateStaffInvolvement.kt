package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole
import java.util.Optional

@Schema(
  description = "Update an involved member of staff in an incident report",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class UpdateStaffInvolvement(
  @param:Schema(
    description = "Username, absent for manually-added staff or those without NOMIS/DPS accounts" +
      "– omit to preserve existing username, provide null to clear it",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 3,
    maxLength = 120,
  )
  val staffUsername: Optional<
    @Size(min = 3, max = 120)
    String,
    >? = null,
  @param:Schema(
    description = "First name",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 1,
    maxLength = 255,
  )
  @field:Size(min = 1, max = 255)
  val firstName: String? = null,
  @param:Schema(
    description = "Surname",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 1,
    maxLength = 255,
  )
  @field:Size(min = 1, max = 255)
  val lastName: String? = null,
  @param:Schema(
    description = "Their role",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val staffRole: StaffRole? = null,
  @param:Schema(
    description = "Optional comment on staff member involvement " +
      "– omit to preserve existing comment, provide null to clear it",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
  )
  val comment: Optional<String>? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    staffUsername == null &&
      firstName == null &&
      lastName == null &&
      staffRole == null &&
      comment == null
}
