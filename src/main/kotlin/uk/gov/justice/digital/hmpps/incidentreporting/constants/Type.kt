package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/*
TODO: DRONE2 has just been added to start in Sept 2024,
  but notably, DRONE1 is still active as well!
  1) need to check in Sept what remains active
  2) need to check start/end dates for all types in NOMIS db to ensure descriptions make sense. eg. DRONE vs DRONE1 vs DRONE2
*/

enum class Type(
  val description: String,
  val active: Boolean = true,
) {
  // active
  ABSCONDER("Absconder"),
  ASSAULT("Assault"),
  ATTEMPTED_ESCAPE_FROM_CUSTODY("Attempted escape from custody"),
  ATTEMPTED_ESCAPE_FROM_ESCORT("Attempted escape from escort"),
  BOMB_THREAT("Bomb threat"),
  BREACH_OF_SECURITY("Breach of security"),
  DAMAGE("Damage"),
  DEATH_IN_CUSTODY("Death in custody"),
  DEATH_OTHER("Death (other)"),
  DISORDER("Disorder"),
  DRONE_SIGHTING("Drone sighting"),
  OLD_DRONE_SIGHTING1("Drone sighting"),
  ESCAPE_FROM_CUSTODY("Escape from custody"),
  ESCAPE_FROM_ESCORT("Escape from escort"),
  FINDS("Finds"),
  FIRE("Fire"),
  FOOD_REFUSAL("Food refusal"),
  FULL_CLOSE_DOWN_SEARCH("Full close down search"),
  KEY_LOCK_INCIDENT("Key lock incident"),
  MISCELLANEOUS("Miscellaneous"),
  RADIO_COMPROMISE("Radio compromise"),
  RELEASED_IN_ERROR("Released in error"),
  SELF_HARM("Self harm"),
  TEMPORARY_RELEASE_FAILURE("Temporary release failure"),
  TOOL_LOSS("Tool loss"),

  // inactive
  OLD_ASSAULT("Assault", active = false),
  OLD_ASSAULT1("Assault (from April 2017)", active = false),
  OLD_ASSAULT2("Assault (from April 2017)", active = false),
  OLD_ASSAULT3("Assault (from April 2017)", active = false),
  OLD_BARRICADE("Barricade/prevention of access", active = false),
  OLD_CONCERTED_INDISCIPLINE("Concerted indiscipline", active = false),
  OLD_DISORDER("Disorder", active = false),
  OLD_DRONE_SIGHTING("Drone sighting", active = false),
  OLD_DRUGS("Drugs", active = false),
  OLD_FINDS("Finds", active = false),
  OLD_FINDS1("Finds (from August 2015)", active = false),
  OLD_FINDS2("Finds (from September 2015)", active = false),
  OLD_FINDS3("Finds (from March 2022)", active = false),
  OLD_FINDS4("Finds (from September 2016)", active = false),
  OLD_FIREARM_ETC("Firearm/ammunition/chemical incapacitant", active = false),
  OLD_HOSTAGE("Hostage", active = false),
  OLD_KEY_LOCK_INCIDENT("Key lock incident", active = false),
  OLD_MOBILES("Mobile phones", active = false),
  OLD_ROOF_CLIMB("Incident at height", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE("Temporary release failure", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE1("Temporary release failure (from July 2015)", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE2("Temporary release failure (from April 2016)", active = false),
  ;

  companion object {
    fun fromNomisCode(type: String): Type = when (type) {
      // active
      "ABSCOND" -> ABSCONDER
      "ASSAULTS3" -> ASSAULT
      "ATT_ESC_E" -> ATTEMPTED_ESCAPE_FROM_ESCORT
      "ATT_ESCAPE" -> ATTEMPTED_ESCAPE_FROM_CUSTODY
      "BOMB" -> BOMB_THREAT
      "BREACH" -> BREACH_OF_SECURITY
      "CLOSE_DOWN" -> FULL_CLOSE_DOWN_SEARCH
      "DAMAGE" -> DAMAGE
      "DEATH_NI" -> DEATH_OTHER
      "DEATH" -> DEATH_IN_CUSTODY
      "DISORDER1" -> DISORDER
      "DRONE1" -> OLD_DRONE_SIGHTING1
      "DRONE2" -> DRONE_SIGHTING
      "ESCAPE_ESC" -> ESCAPE_FROM_ESCORT
      "ESCAPE_EST" -> ESCAPE_FROM_CUSTODY
      "FIND0422" -> FINDS
      "FIRE" -> FIRE
      "FOOD_REF" -> FOOD_REFUSAL
      "KEY_LOCKNEW" -> KEY_LOCK_INCIDENT
      "MISC" -> MISCELLANEOUS
      "RADIO_COMP" -> RADIO_COMPROMISE
      "REL_ERROR" -> RELEASED_IN_ERROR
      "SELF_HARM" -> SELF_HARM
      "TOOL_LOSS" -> TOOL_LOSS
      "TRF3" -> TEMPORARY_RELEASE_FAILURE
      // inactive
      "ASSAULT" -> OLD_ASSAULT
      "ASSAULTS" -> OLD_ASSAULT1
      "ASSAULTS1" -> OLD_ASSAULT2
      "ASSAULTS2" -> OLD_ASSAULT3
      "BARRICADE" -> OLD_BARRICADE
      "CON_INDISC" -> OLD_CONCERTED_INDISCIPLINE
      "DISORDER" -> OLD_DISORDER
      "DRONE" -> OLD_DRONE_SIGHTING
      "DRUGS" -> OLD_DRUGS
      "FINDS" -> OLD_FINDS
      "FIND" -> OLD_FINDS1
      "FIND1" -> OLD_FINDS2
      "FIND0322" -> OLD_FINDS3
      "FINDS1" -> OLD_FINDS4
      "FIREARM_ETC" -> OLD_FIREARM_ETC
      "HOSTAGE" -> OLD_HOSTAGE
      "KEY_LOCK" -> OLD_KEY_LOCK_INCIDENT
      "MOBILES" -> OLD_MOBILES
      "ROOF_CLIMB" -> OLD_ROOF_CLIMB
      "TRF" -> OLD_TEMPORARY_RELEASE_FAILURE
      "TRF1" -> OLD_TEMPORARY_RELEASE_FAILURE1
      "TRF2" -> OLD_TEMPORARY_RELEASE_FAILURE2
      else -> throw ValidationException("Unknown NOMIS incident type: $type")
    }
  }
}
