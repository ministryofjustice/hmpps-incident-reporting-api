package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

// TODO: need to check start/end dates for all types in NOMIS db to ensure descriptions make sense. eg. ASSAULT*

/**
 * The type of reportable incident.
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed, only deactivated, to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class Type(
  val description: String,
  val nomisType: String?,
  val active: Boolean = true,
) {
  // active
  ABSCONDER("Absconder", "ABSCOND"),
  ASSAULT("Assault", "ASSAULTS3"),
  ATTEMPTED_ESCAPE_FROM_CUSTODY("Attempted escape from custody", "ATT_ESCAPE"),
  ATTEMPTED_ESCAPE_FROM_ESCORT("Attempted escape from escort", "ATT_ESC_E"),
  BOMB_THREAT("Bomb threat", "BOMB"),
  BREACH_OF_SECURITY("Breach of security", "BREACH"),
  DEATH_IN_CUSTODY("Death in custody", "DEATH"),
  DEATH_OTHER("Death (other)", "DEATH_NI"),
  DISORDER("Disorder", "DISORDER1"),
  DRONE_SIGHTING("Drone sighting", "DRONE2"),
  ESCAPE_FROM_CUSTODY("Escape from custody", "ESCAPE_EST"),
  ESCAPE_FROM_ESCORT("Escape from escort", "ESCAPE_ESC"),
  FINDS("Finds", "FIND0422"),
  FIRE("Fire", "FIRE"),
  FOOD_REFUSAL("Food refusal", "FOOD_REF"),
  FULL_CLOSE_DOWN_SEARCH("Full close down search", "CLOSE_DOWN"),
  KEY_LOCK_INCIDENT("Key lock incident", "KEY_LOCKNEW"),
  MISCELLANEOUS("Miscellaneous", "MISC"),
  RADIO_COMPROMISE("Radio compromise", "RADIO_COMP"),
  RELEASED_IN_ERROR("Released in error", "REL_ERROR"),
  SELF_HARM("Self harm", "SELF_HARM"),
  TEMPORARY_RELEASE_FAILURE("Temporary release failure", "TRF3"),
  TOOL_LOSS("Tool loss", "TOOL_LOSS"),

  // inactive
  DAMAGE("Damage", "DAMAGE", active = false),
  OLD_ASSAULT("Assault", "ASSAULT", active = false),
  OLD_ASSAULT1("Assault (from April 2017)", "ASSAULTS", active = false),
  OLD_ASSAULT2("Assault (from April 2017)", "ASSAULTS1", active = false),
  OLD_ASSAULT3("Assault (from April 2017)", "ASSAULTS2", active = false),
  OLD_BARRICADE("Barricade/prevention of access", "BARRICADE", active = false),
  OLD_CONCERTED_INDISCIPLINE("Concerted indiscipline", "CON_INDISC", active = false),
  OLD_DISORDER("Disorder", "DISORDER", active = false),
  OLD_DRONE_SIGHTING("Drone sighting", "DRONE", active = false),
  OLD_DRONE_SIGHTING1("Drone sighting (from 2017)", "DRONE1", active = false),
  OLD_DRUGS("Drugs", "DRUGS", active = false),
  OLD_FINDS("Finds", "FINDS", active = false),
  OLD_FINDS1("Finds (from August 2015)", "FIND", active = false),
  OLD_FINDS2("Finds (from September 2015)", "FIND1", active = false),
  OLD_FINDS3("Finds (from March 2022)", "FIND0322", active = false),
  OLD_FINDS4("Finds (from September 2016)", "FINDS1", active = false),
  OLD_FIREARM_ETC("Firearm/ammunition/chemical incapacitant", "FIREARM_ETC", active = false),
  OLD_HOSTAGE("Hostage", "HOSTAGE", active = false),
  OLD_KEY_LOCK_INCIDENT("Key lock incident", "KEY_LOCK", active = false),
  OLD_MOBILES("Mobile phones", "MOBILES", active = false),
  OLD_ROOF_CLIMB("Incident at height", "ROOF_CLIMB", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE("Temporary release failure", "TRF", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE1("Temporary release failure (from July 2015)", "TRF1", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE2("Temporary release failure (from April 2016)", "TRF2", active = false),
  ;

  companion object {
    fun fromNomisCode(type: String): Type = entries.find { it.nomisType == type }
      ?: throw ValidationException("Unknown NOMIS incident type: $type")
  }
}
