package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole

@Schema(description = "Member of staff involved in an incident", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StaffInvolvement(
  // TODO: sequences are only being exposed while we sort out sync problems: they do not need to remain in the api contract
  @param:Schema(description = "Sequence of the staff involvement for this report", deprecated = true)
  val sequence: Int,
  @param:Schema(description = "Username, absent for manually-added staff or those without NOMIS/DPS accounts")
  val staffUsername: String?,
  @param:Schema(description = "First name")
  var firstName: String,
  @param:Schema(description = "Surname")
  var lastName: String,
  @param:Schema(description = "Their role")
  val staffRole: StaffRole,
  @param:Schema(description = "Optional comment on staff member involvement", nullable = true)
  val comment: String? = null,
)
