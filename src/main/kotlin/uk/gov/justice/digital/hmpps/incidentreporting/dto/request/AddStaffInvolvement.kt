package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole

@Schema(
  description = "Add an involved member of staff to an incident report",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class AddStaffInvolvement(
  @param:Schema(
    description = "Username, absent for manually-added staff or those without NOMIS/DPS accounts",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 3,
    maxLength = 120,
  )
  @field:Size(min = 3, max = 120)
  val staffUsername: String? = null,
  @param:Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 255)
  @field:Size(min = 1, max = 255)
  val firstName: String,
  @param:Schema(description = "Surname", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 255)
  @field:Size(min = 1, max = 255)
  val lastName: String,
  @param:Schema(description = "Their role", requiredMode = Schema.RequiredMode.REQUIRED)
  val staffRole: StaffRole,
  @param:Schema(
    description = "Optional comment on staff member involvement",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val comment: String? = null,
)
