package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.validation.ValidationException

enum class IncidentType(
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
  "SELF_HARM" -> IncidentType.SELF_HARM
  "MISC" -> IncidentType.MISCELLANEOUS
  "ASSAULTS3" -> IncidentType.ASSAULT
  "DAMAGE" -> IncidentType.DAMAGE
  "FIND0422" -> IncidentType.FINDS
  "KEY_LOCKNEW" -> IncidentType.KEY_LOCK_INCIDENT
  "DISORDER1" -> IncidentType.DISORDER
  "FIRE" -> IncidentType.FIRE
  "TOOL_LOSS" -> IncidentType.TOOL_LOSS
  "FOOD_REF" -> IncidentType.FOOD_REFUSAL
  "DEATH" -> IncidentType.DEATH_IN_CUSTODY
  "TRF3" -> IncidentType.TEMPORARY_RELEASE_FAILURE
  "RADIO_COMP" -> IncidentType.RADIO_COMPROMISE
  "DRONE1" -> IncidentType.DRONE_SIGHTING
  "ABSCOND" -> IncidentType.ABSCONDER
  "REL_ERROR" -> IncidentType.RELEASED_IN_ERROR
  "BOMB" -> IncidentType.BOMB_THREAT
  "CLOSE_DOWN" -> IncidentType.FULL_CLOSE_DOWN_SEARCH
  "BREACH" -> IncidentType.BREACH_OF_SECURITY
  "DEATH_NI" -> IncidentType.DEATH_OTHER
  "ESCAPE_EST" -> IncidentType.ESCAPE_FROM_CUSTODY
  "ATT_ESCAPE" -> IncidentType.ATTEMPTED_ESCAPE_FROM_CUSTODY
  "ESCAPE_ESC" -> IncidentType.ESCAPE_FROM_ESCORT
  "ATT_ESC_E" -> IncidentType.ATTEMPTED_ESCAPE_FROM_ESCORT

  else -> throw ValidationException("Unknown incident type: $type")
}
