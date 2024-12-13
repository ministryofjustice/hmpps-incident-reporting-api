package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisOffender(
  @Schema(description = "NOMIS id")
  val offenderNo: String,
  @Schema(description = "First name of offender")
  val firstName: String,
  @Schema(description = "Last name of offender")
  val lastName: String,
)
