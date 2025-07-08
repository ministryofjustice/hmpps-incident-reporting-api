package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisOffender(
  @param:Schema(description = "NOMIS id")
  val offenderNo: String,
  @param:Schema(description = "First name of offender")
  val firstName: String,
  @param:Schema(description = "Last name of offender")
  val lastName: String,
)
