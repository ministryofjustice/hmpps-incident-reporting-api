package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/**
 * The outcome of a prisoner’s involvement in an incident.
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class PrisonerOutcome(
  val description: String,
  val nomisCode: String,
) {
  ACCT("ACCT", "ACCT"),
  CHARGED_BY_POLICE("Charged by Police", "CBP"),
  CONVICTED("Convicted", "CON"),
  CORONER_INFORMED("Coroner informed", "CORIN"),
  DEATH("Death", "DEA"),
  FURTHER_CHARGES("Further charges", "FCHRG"),
  LOCAL_INVESTIGATION("Investigation (local)", "ILOC"),
  NEXT_OF_KIN_INFORMED("Next of kin informed", "NKI"),
  PLACED_ON_REPORT("Placed on report", "POR"),
  POLICE_INVESTIGATION("Investigation (Police)", "IPOL"),
  REMAND("Remand", "RMND"),
  SEEN_DUTY_GOV("Seen by Duty Governor", "DUTGOV"),
  SEEN_HEALTHCARE("Seen by Healthcare", "HELTH"),
  SEEN_IMB("Seen by IMB", "IMB"),
  SEEN_OUTSIDE_HOSP("Seen by outside hospital", "OUTH"),
  TRANSFER("Transfer", "TRN"),
  TRIAL("Trial", "TRL"),
  ;

  companion object {
    fun fromNomisCode(code: String): PrisonerOutcome = entries.find { it.nomisCode == code }
      ?: throw ValidationException("Unknown NOMIS prisoner outcome code: $code")
  }
}
