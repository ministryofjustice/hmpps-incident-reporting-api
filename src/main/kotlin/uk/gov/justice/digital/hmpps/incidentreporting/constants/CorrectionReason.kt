package uk.gov.justice.digital.hmpps.incidentreporting.constants

enum class CorrectionReason(
  val description: String,
) {
  MISTAKE("Mistake"),
  INCORRECT_INFORMATION("Incorrect information"),
  MISSING_INFORMATION("Missing information"),
  OTHER("Other reason"),

  // NOMIS did not have this concept so migrated report correction requests become:
  NOT_SPECIFIED("Not specified"),
}
