package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisCode(
  @param:Schema(description = "Code")
  val code: String,
  @param:Schema(description = "Description")
  val description: String,
)
