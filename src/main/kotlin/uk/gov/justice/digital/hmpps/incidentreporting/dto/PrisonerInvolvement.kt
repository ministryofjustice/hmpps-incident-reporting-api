package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole

@Schema(description = "Prisoner involved in an incident", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class PrisonerInvolvement(
  // TODO: sequences are only being exposed while we sort out sync problems: they do not need to remain in the api contract
  @param:Schema(description = "Sequence of the prisoner involvement for this report", deprecated = true)
  val sequence: Int,
  @param:Schema(description = "Prisoner’s NOMIS number")
  val prisonerNumber: String,
  @param:Schema(description = "First name")
  var firstName: String,
  @param:Schema(description = "Surname")
  var lastName: String,
  @param:Schema(description = "Their role")
  val prisonerRole: PrisonerRole,
  @param:Schema(description = "Optional outcome of prisoner’s involvement", nullable = true)
  val outcome: PrisonerOutcome? = null,
  @param:Schema(description = "Optional comment on prisoner’s involvement", nullable = true)
  val comment: String? = null,
)
