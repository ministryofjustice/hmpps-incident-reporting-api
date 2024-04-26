package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisStatus(
  @Schema(description = "Status Code")
  val code: String,
  @Schema(description = "Status Description")
  val description: String,
)
