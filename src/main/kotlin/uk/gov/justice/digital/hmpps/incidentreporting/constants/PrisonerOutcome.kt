package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class PrisonerOutcome(
  val description: String,
) {
  ACCT("ACCT"),
  CHARGED_BY_POLICE("Charged by Police"),
  CONVICTED("Convicted"),
  CORONER_INFORMED("Coroner Informed"),
  DEATH("Death"),
  SEEN_DUTY_GOV("Seen by Duty Governor"),
  FURTHER_CHARGES("Further charges"),
  SEEN_HEALTHCARE("Seen by Healthcare"),
  LOCAL_INVESTIGATION("Investigation (Local)"),
  SEEN_IMB("Seen by IMB"),
  POLICE_INVESTIGATION("Investigation (Police)"),
  NEXT_OF_KIN_INFORMED("Next of kin informed"),
  SEEN_OUTSIDE_HOSP("Seen by outside hospital"),
  PLACED_ON_REPORT("Placed on report"),
  REMAND("Remand"),
  TRIAL("Trial"),
  TRANSFER("Transfer"),
  ;

  companion object {
    fun fromNomisCode(outcome: String): PrisonerOutcome = when (outcome) {
      "ACCT" -> ACCT
      "CBP" -> CHARGED_BY_POLICE
      "CON" -> CONVICTED
      "CORIN" -> CORONER_INFORMED
      "DEA" -> DEATH
      "DUTGOV" -> SEEN_DUTY_GOV
      "FCHRG" -> FURTHER_CHARGES
      "HELTH" -> SEEN_HEALTHCARE
      "ILOC" -> LOCAL_INVESTIGATION
      "IMB" -> SEEN_IMB
      "IPOL" -> POLICE_INVESTIGATION
      "NKI" -> NEXT_OF_KIN_INFORMED
      "OUTH" -> SEEN_OUTSIDE_HOSP
      "POR" -> PLACED_ON_REPORT
      "RMND" -> REMAND
      "TRL" -> TRIAL
      "TRN" -> TRANSFER
      else -> throw ValidationException("Unknown NOMIS prisoner outcome: $outcome")
    }
  }
}
