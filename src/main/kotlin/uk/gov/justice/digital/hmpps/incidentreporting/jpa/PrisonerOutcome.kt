package uk.gov.justice.digital.hmpps.incidentreporting.jpa

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
