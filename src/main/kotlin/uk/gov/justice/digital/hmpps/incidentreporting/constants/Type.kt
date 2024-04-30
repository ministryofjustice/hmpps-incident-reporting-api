package uk.gov.justice.digital.hmpps.incidentreporting.constants

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
  ;

  companion object {
    fun fromNomisCode(type: String): Type = when (type) {
      "SELF_HARM" -> SELF_HARM
      "MISC" -> MISCELLANEOUS
      "ASSAULTS3" -> ASSAULT
      "DAMAGE" -> DAMAGE
      "FIND0422" -> FINDS
      "KEY_LOCKNEW" -> KEY_LOCK_INCIDENT
      "DISORDER1" -> DISORDER
      "FIRE" -> FIRE
      "TOOL_LOSS" -> TOOL_LOSS
      "FOOD_REF" -> FOOD_REFUSAL
      "DEATH" -> DEATH_IN_CUSTODY
      "TRF3" -> TEMPORARY_RELEASE_FAILURE
      "RADIO_COMP" -> RADIO_COMPROMISE
      "DRONE1" -> DRONE_SIGHTING
      "ABSCOND" -> ABSCONDER
      "REL_ERROR" -> RELEASED_IN_ERROR
      "BOMB" -> BOMB_THREAT
      "CLOSE_DOWN" -> FULL_CLOSE_DOWN_SEARCH
      "BREACH" -> BREACH_OF_SECURITY
      "DEATH_NI" -> DEATH_OTHER
      "ESCAPE_EST" -> ESCAPE_FROM_CUSTODY
      "ATT_ESCAPE" -> ATTEMPTED_ESCAPE_FROM_CUSTODY
      "ESCAPE_ESC" -> ESCAPE_FROM_ESCORT
      "ATT_ESC_E" -> ATTEMPTED_ESCAPE_FROM_ESCORT
      else -> throw ValidationException("Unknown NOMIS incident type: $type")
    }
  }
}
