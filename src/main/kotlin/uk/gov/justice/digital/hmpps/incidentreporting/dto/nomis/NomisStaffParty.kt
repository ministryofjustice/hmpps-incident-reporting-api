package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisStaffParty(
  @Schema(description = "Staff involved in the incident")
  val staff: NomisStaff,
  @Schema(description = "Staff role in the incident")
  val role: NomisCode,
  @Schema(description = "General information about the incident")
  val comment: String?,
)
