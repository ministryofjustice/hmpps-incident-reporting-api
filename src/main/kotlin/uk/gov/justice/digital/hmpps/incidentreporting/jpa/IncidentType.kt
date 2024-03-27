package uk.gov.justice.digital.hmpps.incidentreporting.jpa

enum class IncidentType(
  val description: String,
) {
  SELF_HARM("Self Harm"),
  ASSAULT("Assault"),
  DAMAGE("Damage"),
  FINDS("Finds"),
  KEY_LOCK_INCIDENT("Key Lock Incident"),
  DISORDER("Disorder"),
  DRONE_SIGHTING(""),
  FIRE(""),
  TOOL_LOSS(""),
  FOOD_REFUSAL(""),
  DEATH_IN_CUSTODY(""),
  TEMPORARY_RELEASE_FAILURE(""),
  RADIO_COMPROMISE(""),
  ABSCONDER(""),
  RELEASED_IN_ERROR(""),
  BOMB_THREAT(""),
  FULL_CLOSE_DOWN_SEARCH(""),
  BREACH_OF_SECURITY(""),
  DEATH_OTHER(""),
  ATTEMPTED_ESCAPE_FROM_CUSTODY(""),
  ESCAPE_FROM_CUSTODY(""),
  ATTEMPTED_ESCAPE_FROM_ESCORT(""),
  ESCAPE_FROM_ESCORT(""),
  MISCELLANEOUS(""),
}
