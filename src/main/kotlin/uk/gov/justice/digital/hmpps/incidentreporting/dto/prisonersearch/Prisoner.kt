package uk.gov.justice.digital.hmpps.incidentreporting.dto.prisonersearch

data class Prisoner(
  val prisonerNumber: String,
  val firstName: String,
  val middleNames: String? = null,
  val lastName: String,

  val prisonId: String? = null,
  val prisonName: String? = null,
  val cellLocation: String? = null,
)
