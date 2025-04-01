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
  val typeFamily: TypeFamily,
  val description: String,
  val nomisType: String?,
  val active: Boolean = true,
) {
  // active
  ABSCONDER(TypeFamily.ABSCOND, "Absconder", "ABSCOND"),
  ASSAULT(TypeFamily.ASSAULT, "Assault", "ASSAULTS3"),
  ATTEMPTED_ESCAPE_FROM_CUSTODY(TypeFamily.ATTEMPTED_ESCAPE_FROM_PRISON, "Attempted escape from custody", "ATT_ESCAPE"),
  ATTEMPTED_ESCAPE_FROM_ESCORT(TypeFamily.ATTEMPTED_ESCAPE_FROM_ESCORT, "Attempted escape from escort", "ATT_ESC_E"),
  BOMB_THREAT(TypeFamily.BOMB, "Bomb threat", "BOMB"),
  BREACH_OF_SECURITY(TypeFamily.BREACH_OF_SECURITY, "Breach of security", "BREACH"),
  DEATH_IN_CUSTODY(TypeFamily.DEATH_PRISONER, "Death in custody", "DEATH"),
  DEATH_OTHER(TypeFamily.DEATH_OTHER, "Death (other)", "DEATH_NI"),
  DISORDER(TypeFamily.DISORDER, "Disorder", "DISORDER1"),
  DRONE_SIGHTING(TypeFamily.DRONE_SIGHTING, "Drone sighting", "DRONE2"),
  ESCAPE_FROM_CUSTODY(TypeFamily.ESCAPE_FROM_PRISON, "Escape from custody", "ESCAPE_EST"),
  ESCAPE_FROM_ESCORT(TypeFamily.ESCAPE_FROM_ESCORT, "Escape from escort", "ESCAPE_ESC"),
  FINDS(TypeFamily.FIND, "Finds", "FIND0422"),
  FIRE(TypeFamily.FIRE, "Fire", "FIRE"),
  FOOD_REFUSAL(TypeFamily.FOOD_REFUSAL, "Food refusal", "FOOD_REF"),
  FULL_CLOSE_DOWN_SEARCH(TypeFamily.CLOSE_DOWN_SEARCH, "Full close down search", "CLOSE_DOWN"),
  KEY_LOCK_INCIDENT(TypeFamily.KEY_OR_LOCK, "Key lock incident", "KEY_LOCKNEW"),
  MISCELLANEOUS(TypeFamily.MISCELLANEOUS, "Miscellaneous", "MISC"),
  RADIO_COMPROMISE(TypeFamily.RADIO_COMPROMISE, "Radio compromise", "RADIO_COMP"),
  RELEASED_IN_ERROR(TypeFamily.RELEASE_IN_ERROR, "Released in error", "REL_ERROR"),
  SELF_HARM(TypeFamily.SELF_HARM, "Self harm", "SELF_HARM"),
  TEMPORARY_RELEASE_FAILURE(TypeFamily.TEMPORARY_RELEASE_FAILURE, "Temporary release failure", "TRF3"),
  TOOL_LOSS(TypeFamily.TOOL_LOSS, "Tool loss", "TOOL_LOSS"),

  // inactive
  DAMAGE(TypeFamily.DAMAGE, "Damage", "DAMAGE", active = false),
  OLD_ASSAULT(TypeFamily.ASSAULT, "Assault", "ASSAULT", active = false),
  OLD_ASSAULT1(TypeFamily.ASSAULT, "Assault (from April 2017)", "ASSAULTS", active = false),
  OLD_ASSAULT2(TypeFamily.ASSAULT, "Assault (from April 2017)", "ASSAULTS1", active = false),
  OLD_ASSAULT3(TypeFamily.ASSAULT, "Assault (from April 2017)", "ASSAULTS2", active = false),
  OLD_BARRICADE(TypeFamily.BARRICADE, "Barricade/prevention of access", "BARRICADE", active = false),
  OLD_CONCERTED_INDISCIPLINE(TypeFamily.CONCERTED_INDISCIPLINE, "Concerted indiscipline", "CON_INDISC", active = false),
  OLD_DISORDER(TypeFamily.DISORDER, "Disorder", "DISORDER", active = false),
  OLD_DRONE_SIGHTING(TypeFamily.DRONE_SIGHTING, "Drone sighting", "DRONE", active = false),
  OLD_DRONE_SIGHTING1(TypeFamily.DRONE_SIGHTING, "Drone sighting (from 2017)", "DRONE1", active = false),
  OLD_DRUGS(TypeFamily.DRUGS, "Drugs", "DRUGS", active = false),
  OLD_FINDS(TypeFamily.FIND, "Finds", "FINDS", active = false),
  OLD_FINDS1(TypeFamily.FIND, "Finds (from August 2015)", "FIND", active = false),
  OLD_FINDS2(TypeFamily.FIND, "Finds (from September 2015)", "FIND1", active = false),
  OLD_FINDS3(TypeFamily.FIND, "Finds (from March 2022)", "FIND0322", active = false),
  OLD_FINDS4(TypeFamily.FIND, "Finds (from September 2016)", "FINDS1", active = false),
  OLD_FIREARM_ETC(TypeFamily.FIREARM, "Firearm/ammunition/chemical incapacitant", "FIREARM_ETC", active = false),
  OLD_HOSTAGE(TypeFamily.HOSTAGE, "Hostage", "HOSTAGE", active = false),
  OLD_KEY_LOCK_INCIDENT(TypeFamily.KEY_OR_LOCK, "Key lock incident", "KEY_LOCK", active = false),
  OLD_MOBILES(TypeFamily.MOBILE_PHONE, "Mobile phones", "MOBILES", active = false),
  OLD_ROOF_CLIMB(TypeFamily.INCIDENT_AT_HEIGHT, "Incident at height", "ROOF_CLIMB", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE(
    TypeFamily.TEMPORARY_RELEASE_FAILURE,
    "Temporary release failure",
    "TRF",
    active = false,
  ),
  OLD_TEMPORARY_RELEASE_FAILURE1(
    TypeFamily.TEMPORARY_RELEASE_FAILURE,
    "Temporary release failure (from July 2015)",
    "TRF1",
    active = false,
  ),
  OLD_TEMPORARY_RELEASE_FAILURE2(
    TypeFamily.TEMPORARY_RELEASE_FAILURE,
    "Temporary release failure (from April 2016)",
    "TRF2",
    active = false,
  ),
  ;

  companion object {
    fun fromNomisCode(type: String): Type = entries.find { it.nomisType == type }
      ?: throw ValidationException("Unknown NOMIS incident type: $type")
  }
}
