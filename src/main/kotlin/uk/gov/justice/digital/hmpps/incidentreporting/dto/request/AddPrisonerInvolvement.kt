package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole

@Schema(description = "Add an involved prisoner to an incident report", accessMode = Schema.AccessMode.WRITE_ONLY)
data class AddPrisonerInvolvement(
  @param:Schema(
    description = "Prisoner’s NOMIS number",
    requiredMode = Schema.RequiredMode.REQUIRED,
    minLength = 7,
    maxLength = 10,
  )
  @field:Size(min = 7, max = 10)
  val prisonerNumber: String,
  @param:Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 255)
  @field:Size(min = 1, max = 255)
  val firstName: String,
  @param:Schema(description = "Surname", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 255)
  @field:Size(min = 1, max = 255)
  val lastName: String,
  @param:Schema(description = "Their role", requiredMode = Schema.RequiredMode.REQUIRED)
  val prisonerRole: PrisonerRole,
  @param:Schema(
    description = "Optional outcome of prisoner’s involvement",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val outcome: PrisonerOutcome? = null,
  @param:Schema(
    description = "Optional comment on prisoner’s involvement",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
    defaultValue = "null",
  )
  val comment: String? = null,
)
