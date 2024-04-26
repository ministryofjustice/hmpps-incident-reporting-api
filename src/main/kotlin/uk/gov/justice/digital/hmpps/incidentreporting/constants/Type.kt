package uk.gov.justice.digital.hmpps.incidentreporting.constants

enum class Type(
  val description: String,
) {
  SELF_HARM("Self Harm"),
  ASSAULT("Assault"),
  DAMAGE("Damage"),
  FINDS("Finds"),
  KEY_LOCK_INCIDENT("Key Lock Incident"),
  DISORDER("Disorder"),
  DRONE_SIGHTING("Drone Sighting"),
  FIRE("Fire"),
  TOOL_LOSS("Tool Loss"),
  FOOD_REFUSAL("Food Refusal"),
  DEATH_IN_CUSTODY("Death In Custody"),
  TEMPORARY_RELEASE_FAILURE("Temporary Release Failure"),
  RADIO_COMPROMISE("Radio Compromise"),
  ABSCONDER("Absconder"),
  RELEASED_IN_ERROR("Released In Error"),
  BOMB_THREAT("Bomb Threat"),
  FULL_CLOSE_DOWN_SEARCH("Full Close Down Search"),
  BREACH_OF_SECURITY("Breach Of Security"),
  DEATH_OTHER("Death (Other)"),
  ATTEMPTED_ESCAPE_FROM_CUSTODY("Attempted Escape From Custody"),
  ESCAPE_FROM_CUSTODY("Escape From Custody"),
  ATTEMPTED_ESCAPE_FROM_ESCORT("Attempted Escape From Escort"),
  ESCAPE_FROM_ESCORT("Escape From Escort"),
  MISCELLANEOUS("Miscellaneous"),
}
