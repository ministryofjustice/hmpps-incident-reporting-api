package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisOffenderParty(
  @Schema(description = "Offender involved in the incident")
  val offender: NomisOffender,
  @Schema(description = "Offender role in the incident")
  val role: NomisCode,
  @Schema(description = "The outcome of the incident")
  val outcome: NomisCode?,
  @Schema(description = "General information about the incident")
  val comment: String?,
)
