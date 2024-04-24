package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.validation.ValidationException

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

fun convertIncidentType(type: String) = when (type) {
  "SELF_HARM" -> Type.SELF_HARM
  "MISC" -> Type.MISCELLANEOUS
  "ASSAULTS3" -> Type.ASSAULT
  "DAMAGE" -> Type.DAMAGE
  "FIND0422" -> Type.FINDS
  "KEY_LOCKNEW" -> Type.KEY_LOCK_INCIDENT
  "DISORDER1" -> Type.DISORDER
  "FIRE" -> Type.FIRE
  "TOOL_LOSS" -> Type.TOOL_LOSS
  "FOOD_REF" -> Type.FOOD_REFUSAL
  "DEATH" -> Type.DEATH_IN_CUSTODY
  "TRF3" -> Type.TEMPORARY_RELEASE_FAILURE
  "RADIO_COMP" -> Type.RADIO_COMPROMISE
  "DRONE1" -> Type.DRONE_SIGHTING
  "ABSCOND" -> Type.ABSCONDER
  "REL_ERROR" -> Type.RELEASED_IN_ERROR
  "BOMB" -> Type.BOMB_THREAT
  "CLOSE_DOWN" -> Type.FULL_CLOSE_DOWN_SEARCH
  "BREACH" -> Type.BREACH_OF_SECURITY
  "DEATH_NI" -> Type.DEATH_OTHER
  "ESCAPE_EST" -> Type.ESCAPE_FROM_CUSTODY
  "ATT_ESCAPE" -> Type.ATTEMPTED_ESCAPE_FROM_CUSTODY
  "ESCAPE_ESC" -> Type.ESCAPE_FROM_ESCORT
  "ATT_ESC_E" -> Type.ATTEMPTED_ESCAPE_FROM_ESCORT

  else -> throw ValidationException("Unknown incident type: $type")
}
