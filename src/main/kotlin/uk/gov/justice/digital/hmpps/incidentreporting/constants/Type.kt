package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/**
 * The type of reportable incident.
 *
 * NB:
 *   - new items should use their family code and increment the suffix
 *   - items cannot be removed, only deactivated, to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code is expected to be 60 chars max
 */
enum class Type(
  val typeFamily: TypeFamily,
  val nomisType: String?,
  val active: Boolean = true,
) {
  // active
  ABSCOND_1(TypeFamily.ABSCOND, "ABSCOND"),
  ASSAULT_5(TypeFamily.ASSAULT, "ASSAULTS3"),
  ATTEMPTED_ESCAPE_FROM_PRISON_1(TypeFamily.ATTEMPTED_ESCAPE_FROM_PRISON, "ATT_ESCAPE"),
  ATTEMPTED_ESCAPE_FROM_ESCORT_1(TypeFamily.ATTEMPTED_ESCAPE_FROM_ESCORT, "ATT_ESC_E"),
  BOMB_1(TypeFamily.BOMB, "BOMB"),
  BREACH_OF_SECURITY_1(TypeFamily.BREACH_OF_SECURITY, "BREACH"),
  DEATH_PRISONER_1(TypeFamily.DEATH_PRISONER, "DEATH"),
  DEATH_OTHER_1(TypeFamily.DEATH_OTHER, "DEATH_NI"),
  DISORDER_2(TypeFamily.DISORDER, "DISORDER1"),
  DRONE_SIGHTING_3(TypeFamily.DRONE_SIGHTING, "DRONE2"),
  ESCAPE_FROM_PRISON_1(TypeFamily.ESCAPE_FROM_PRISON, "ESCAPE_EST"),
  ESCAPE_FROM_ESCORT_1(TypeFamily.ESCAPE_FROM_ESCORT, "ESCAPE_ESC"),
  FIND_6(TypeFamily.FIND, "FIND0422"),
  FIRE_1(TypeFamily.FIRE, "FIRE"),
  FOOD_REFUSAL_1(TypeFamily.FOOD_REFUSAL, "FOOD_REF"),
  CLOSE_DOWN_SEARCH_1(TypeFamily.CLOSE_DOWN_SEARCH, "CLOSE_DOWN"),
  KEY_OR_LOCK_2(TypeFamily.KEY_OR_LOCK, "KEY_LOCKNEW"),
  MISCELLANEOUS_1(TypeFamily.MISCELLANEOUS, "MISC"),
  RADIO_COMPROMISE_1(TypeFamily.RADIO_COMPROMISE, "RADIO_COMP"),
  RELEASE_IN_ERROR_1(TypeFamily.RELEASE_IN_ERROR, "REL_ERROR"),
  SELF_HARM_1(TypeFamily.SELF_HARM, "SELF_HARM"),
  TEMPORARY_RELEASE_FAILURE_4(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF3"),
  TOOL_LOSS_1(TypeFamily.TOOL_LOSS, "TOOL_LOSS"),

  // inactive
  DAMAGE_1(TypeFamily.DAMAGE, "DAMAGE", active = false),
  ASSAULT_1(TypeFamily.ASSAULT, "ASSAULT", active = false),
  ASSAULT_2(TypeFamily.ASSAULT, "ASSAULTS", active = false),
  ASSAULT_3(TypeFamily.ASSAULT, "ASSAULTS1", active = false),
  ASSAULT_4(TypeFamily.ASSAULT, "ASSAULTS2", active = false),
  BARRICADE_1(TypeFamily.BARRICADE, "BARRICADE", active = false),
  CONCERTED_INDISCIPLINE_1(TypeFamily.CONCERTED_INDISCIPLINE, "CON_INDISC", active = false),
  DISORDER_1(TypeFamily.DISORDER, "DISORDER", active = false),
  DRONE_SIGHTING_1(TypeFamily.DRONE_SIGHTING, "DRONE", active = false),
  DRONE_SIGHTING_2(TypeFamily.DRONE_SIGHTING, "DRONE1", active = false),
  DRUGS_1(TypeFamily.DRUGS, "DRUGS", active = false),
  FIND_1(TypeFamily.FIND, "FINDS", active = false),
  FIND_2(TypeFamily.FIND, "FIND", active = false),
  FIND_3(TypeFamily.FIND, "FIND1", active = false),
  FIND_4(TypeFamily.FIND, "FIND0322", active = false),
  FIND_5(TypeFamily.FIND, "FINDS1", active = false),
  FIREARM_1(TypeFamily.FIREARM, "FIREARM_ETC", active = false),
  HOSTAGE_1(TypeFamily.HOSTAGE, "HOSTAGE", active = false),
  KEY_OR_LOCK_1(TypeFamily.KEY_OR_LOCK, "KEY_LOCK", active = false),
  MOBILE_PHONE_1(TypeFamily.MOBILE_PHONE, "MOBILES", active = false),
  INCIDENT_AT_HEIGHT_1(TypeFamily.INCIDENT_AT_HEIGHT, "ROOF_CLIMB", active = false),
  TEMPORARY_RELEASE_FAILURE_1(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF", active = false),
  TEMPORARY_RELEASE_FAILURE_2(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF1", active = false),
  TEMPORARY_RELEASE_FAILURE_3(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF2", active = false),
  ;

  val description = typeFamily.description

  companion object {
    fun fromNomisCode(type: String): Type = entries.find { it.nomisType == type }
      ?: throw ValidationException("Unknown NOMIS incident type: $type")
  }
}
