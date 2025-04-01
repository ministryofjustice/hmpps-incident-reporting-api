package uk.gov.justice.digital.hmpps.incidentreporting.constants

/**
 * Incident types are grouped into families
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class TypeFamily(
  val description: String,
) {
  ABSCOND("Abscond"),
  ASSAULT("Assault"),
  ATTEMPTED_ESCAPE_FROM_ESCORT("Attempted escape from escort"),
  ATTEMPTED_ESCAPE_FROM_PRISON("Attempted escape from establishment"),
  BARRICADE("Barricade"),
  BOMB("Bomb explosion or threat"),
  BREACH_OF_SECURITY("Breach or attempted breach of security"),
  CLOSE_DOWN_SEARCH("Close down search"),
  CONCERTED_INDISCIPLINE("Incident involving 2 or more prisioners acting together"),
  DAMAGE("Deliberate damage to prison property"),
  DEATH_OTHER("Death of other person"),
  DEATH_PRISONER("Death of prisoner"),
  DISORDER("Disorder"),
  DRONE_SIGHTING("Drone sighting"),
  DRUGS("Drugs"),
  ESCAPE_FROM_ESCORT("Escape from escort"),
  ESCAPE_FROM_PRISON("Escape from establishment"),
  FIND("Find of illicit items"),
  FIRE("Fire"),
  FIREARM("Firearm, ammunition or chemical incapacitant"),
  FOOD_REFUSAL("Food or liquid refusual"),
  HOSTAGE("Hostage incident"),
  INCIDENT_AT_HEIGHT("Incident at height"),
  KEY_OR_LOCK("Key or lock compromise"),
  MISCELLANEOUS("Miscellaneous"),
  MOBILE_PHONE("Mobile phone"),
  RADIO_COMPROMISE("Radio compromise"),
  RELEASE_IN_ERROR("Release in error"),
  SELF_HARM("Self harm"),
  TEMPORARY_RELEASE_FAILURE("Temporary release failure"),
  TOOL_LOSS("Tool or implement loss"),
}
