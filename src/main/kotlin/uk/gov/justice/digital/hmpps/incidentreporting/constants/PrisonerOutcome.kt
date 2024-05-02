package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class PrisonerOutcome(
  val description: String,
) {
  ACCT("ACCT"),
  CHARGED_BY_POLICE("Charged by Police"),
  CONVICTED("Convicted"),
  CORONER_INFORMED("Coroner informed"),
  DEATH("Death"),
  FURTHER_CHARGES("Further charges"),
  LOCAL_INVESTIGATION("Investigation (local)"),
  NEXT_OF_KIN_INFORMED("Next of kin informed"),
  PLACED_ON_REPORT("Placed on report"),
  POLICE_INVESTIGATION("Investigation (Police)"),
  REMAND("Remand"),
  SEEN_DUTY_GOV("Seen by Duty Governor"),
  SEEN_HEALTHCARE("Seen by Healthcare"),
  SEEN_IMB("Seen by IMB"),
  SEEN_OUTSIDE_HOSP("Seen by outside hospital"),
  TRANSFER("Transfer"),
  TRIAL("Trial"),
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
