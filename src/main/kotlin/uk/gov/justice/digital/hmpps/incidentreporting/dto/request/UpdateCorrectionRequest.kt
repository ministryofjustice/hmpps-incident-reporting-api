package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserAction
import uk.gov.justice.digital.hmpps.incidentreporting.constants.UserType
import java.util.Optional

@Schema(description = "Update a correction request in an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class UpdateCorrectionRequest(
  @param:Schema(
    description = "The changes being requested",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 1,
  )
  @field:Size(min = 1)
  val descriptionOfChange: String? = null,
  @param:Schema(
    description = "The location where the staff member is raising the correction",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    minLength = 2,
    maxLength = 20,
  )
  val location: Optional<
    @Size(min = 2, max = 20)
    String,
    >? = null,
  @param:Schema(
    description = "Action taken by the user on the report",
    requiredMode = Schema.RequiredMode.REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val userAction: Optional<UserAction>? = null,
  @param:Schema(
    description = "Reference number of the original report of which this report is a duplicate of",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
    minLength = 1,
    maxLength = 25,
  )
  val originalReportReference: Optional<
    @Size(min = 1, max = 25)
    String,
    >? = null,
  @param:Schema(
    description = "Type of user that submitted this action on the report",
    requiredMode = Schema.RequiredMode.REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val userType: Optional<UserType>? = null,
) {
  @JsonIgnore
  val isEmpty: Boolean =
    descriptionOfChange == null &&
      location == null &&
      userAction == null &&
      originalReportReference == null &&
      userType == null
}
