package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisStaff(
  @param:Schema(description = "Username of first account related to staff")
  val username: String,
  @param:Schema(description = "NOMIS staff id")
  val staffId: Long,
  @param:Schema(description = "First name of staff member")
  val firstName: String,
  @param:Schema(description = "Last name of staff member")
  val lastName: String,
)
