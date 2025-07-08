package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Staff role constant", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StaffRoleConstantDescription(
  @param:Schema(description = "Machine-readable identifier of this value", example = "ACTIVELY_INVOLVED")
  val code: String,
  @param:Schema(description = "Human-readable description of this value", example = "Actively involved")
  val description: String,

  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @param:Schema(
    description = "Machine-readable NOMIS identifiers of this value, which may be empty for newer staff roles",
    example = "[AI, INV]",
    deprecated = true,
  )
  val nomisCodes: List<String>,
)
