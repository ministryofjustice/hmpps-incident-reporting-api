package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/**
 * The type of reportable incident.
 *
 * NB:
 *   - new items should have a reasonably readable code
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
  ABSCONDER(TypeFamily.ABSCOND, "ABSCOND"),
  ASSAULT(TypeFamily.ASSAULT, "ASSAULTS3"),
  ATTEMPTED_ESCAPE_FROM_CUSTODY(TypeFamily.ATTEMPTED_ESCAPE_FROM_PRISON, "ATT_ESCAPE"),
  ATTEMPTED_ESCAPE_FROM_ESCORT(TypeFamily.ATTEMPTED_ESCAPE_FROM_ESCORT, "ATT_ESC_E"),
  BOMB_THREAT(TypeFamily.BOMB, "BOMB"),
  BREACH_OF_SECURITY(TypeFamily.BREACH_OF_SECURITY, "BREACH"),
  DEATH_IN_CUSTODY(TypeFamily.DEATH_PRISONER, "DEATH"),
  DEATH_OTHER(TypeFamily.DEATH_OTHER, "DEATH_NI"),
  DISORDER(TypeFamily.DISORDER, "DISORDER1"),
  DRONE_SIGHTING(TypeFamily.DRONE_SIGHTING, "DRONE2"),
  ESCAPE_FROM_CUSTODY(TypeFamily.ESCAPE_FROM_PRISON, "ESCAPE_EST"),
  ESCAPE_FROM_ESCORT(TypeFamily.ESCAPE_FROM_ESCORT, "ESCAPE_ESC"),
  FINDS(TypeFamily.FIND, "FIND0422"),
  FIRE(TypeFamily.FIRE, "FIRE"),
  FOOD_REFUSAL(TypeFamily.FOOD_REFUSAL, "FOOD_REF"),
  FULL_CLOSE_DOWN_SEARCH(TypeFamily.CLOSE_DOWN_SEARCH, "CLOSE_DOWN"),
  KEY_LOCK_INCIDENT(TypeFamily.KEY_OR_LOCK, "KEY_LOCKNEW"),
  MISCELLANEOUS(TypeFamily.MISCELLANEOUS, "MISC"),
  RADIO_COMPROMISE(TypeFamily.RADIO_COMPROMISE, "RADIO_COMP"),
  RELEASED_IN_ERROR(TypeFamily.RELEASE_IN_ERROR, "REL_ERROR"),
  SELF_HARM(TypeFamily.SELF_HARM, "SELF_HARM"),
  TEMPORARY_RELEASE_FAILURE(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF3"),
  TOOL_LOSS(TypeFamily.TOOL_LOSS, "TOOL_LOSS"),

  // inactive
  DAMAGE(TypeFamily.DAMAGE, "DAMAGE", active = false),
  OLD_ASSAULT(TypeFamily.ASSAULT, "ASSAULT", active = false),
  OLD_ASSAULT1(TypeFamily.ASSAULT, "ASSAULTS", active = false),
  OLD_ASSAULT2(TypeFamily.ASSAULT, "ASSAULTS1", active = false),
  OLD_ASSAULT3(TypeFamily.ASSAULT, "ASSAULTS2", active = false),
  OLD_BARRICADE(TypeFamily.BARRICADE, "BARRICADE", active = false),
  OLD_CONCERTED_INDISCIPLINE(TypeFamily.CONCERTED_INDISCIPLINE, "CON_INDISC", active = false),
  OLD_DISORDER(TypeFamily.DISORDER, "DISORDER", active = false),
  OLD_DRONE_SIGHTING(TypeFamily.DRONE_SIGHTING, "DRONE", active = false),
  OLD_DRONE_SIGHTING1(TypeFamily.DRONE_SIGHTING, "DRONE1", active = false),
  OLD_DRUGS(TypeFamily.DRUGS, "DRUGS", active = false),
  OLD_FINDS(TypeFamily.FIND, "FINDS", active = false),
  OLD_FINDS1(TypeFamily.FIND, "FIND", active = false),
  OLD_FINDS2(TypeFamily.FIND, "FIND1", active = false),
  OLD_FINDS3(TypeFamily.FIND, "FIND0322", active = false),
  OLD_FINDS4(TypeFamily.FIND, "FINDS1", active = false),
  OLD_FIREARM_ETC(TypeFamily.FIREARM, "FIREARM_ETC", active = false),
  OLD_HOSTAGE(TypeFamily.HOSTAGE, "HOSTAGE", active = false),
  OLD_KEY_LOCK_INCIDENT(TypeFamily.KEY_OR_LOCK, "KEY_LOCK", active = false),
  OLD_MOBILES(TypeFamily.MOBILE_PHONE, "MOBILES", active = false),
  OLD_ROOF_CLIMB(TypeFamily.INCIDENT_AT_HEIGHT, "ROOF_CLIMB", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE1(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF1", active = false),
  OLD_TEMPORARY_RELEASE_FAILURE2(TypeFamily.TEMPORARY_RELEASE_FAILURE, "TRF2", active = false),
  ;

  val description = typeFamily.description

  companion object {
    fun fromNomisCode(type: String): Type = entries.find { it.nomisType == type }
      ?: throw ValidationException("Unknown NOMIS incident type: $type")
  }
}
