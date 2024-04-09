package uk.gov.justice.digital.hmpps.incidentreporting.jpa

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
  NEXT_OF_KIN_INFORMED("Next of kin Informed"),
  SEEN_OUTSIDE_HOSP("Seen by Outside Hospital"),
  PLACED_ON_REPORT("Placed on Report"),
  REMAND("Remand"),
  TRIAL("Trial"),
  TRANSFER("Transfer"),
}

fun mapPrisonerOutcome(code: String) = when (code) {
  "ACCT" -> PrisonerOutcome.ACCT
  "CBP" -> PrisonerOutcome.CHARGED_BY_POLICE
  "CON" -> PrisonerOutcome.CONVICTED
  "CORIN" -> PrisonerOutcome.CORONER_INFORMED
  "DEA" -> PrisonerOutcome.DEATH
  "DUTGOV" -> PrisonerOutcome.SEEN_DUTY_GOV
  "FCHRG" -> PrisonerOutcome.FURTHER_CHARGES
  "HELTH" -> PrisonerOutcome.SEEN_HEALTHCARE
  "ILOC" -> PrisonerOutcome.LOCAL_INVESTIGATION
  "IMB" -> PrisonerOutcome.SEEN_IMB
  "IPOL" -> PrisonerOutcome.POLICE_INVESTIGATION
  "NKI" -> PrisonerOutcome.NEXT_OF_KIN_INFORMED
  "OUTH" -> PrisonerOutcome.SEEN_OUTSIDE_HOSP
  "POR" -> PrisonerOutcome.PLACED_ON_REPORT
  "RMND" -> PrisonerOutcome.REMAND
  "TRL" -> PrisonerOutcome.TRIAL
  "TRN" -> PrisonerOutcome.TRANSFER

  else -> throw ValidationException("Unknown prisoner outcome: $code")
}
