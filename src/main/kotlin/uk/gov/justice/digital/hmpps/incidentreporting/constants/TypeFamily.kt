package uk.gov.justice.digital.hmpps.incidentreporting.constants

/**
 * Incident types are grouped into families and share a description.
 *
 * NB:
 *   - new families should have a reasonably readable code
 *   - existing families cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 *   - a family might not have any active types
 */
enum class TypeFamily(
  val description: String,
) {
  ABSCOND("Abscond"),
  ASSAULT("Assault"),
  ATTEMPTED_ESCAPE_FROM_PRISON("Attempted escape from establishment"),
  ATTEMPTED_ESCAPE_FROM_ESCORT("Attempted escape from escort"),
  BARRICADE("Barricade"),
  BC_DISRUPT_3RD_PTY("Disruption to 3rd party supplier - Business Continuity"),
  BC_FUEL_SHORTAGE("Fuel Shortage - Business Continuity"),
  BC_LOSS_ACCESS_EGRESS("Loss of Access / Egress  - Business Continuity"),
  BC_LOSS_COMMS("Loss of Communications & Digital Systems - Business Continuity"),
  BC_LOSS_UTILS("Loss of Utilities - Business Continuity"),
  BC_SERV_WEATHER("Severe Weather - Business Continuity"),
  BC_STAFF_SHORTAGES("Staff shortages  - Business Continuity"),
  BC_WIDESPREAD_ILLNESS("Widespread illness - Business Continuity"),
  BOMB("Bomb explosion or threat"),
  BREACH_OF_SECURITY("Breach or attempted breach of security"),
  CLOSE_DOWN_SEARCH("Close down search"),
  CONCERTED_INDISCIPLINE("Incident involving 2 or more prisioners acting together"),
  DAMAGE("Deliberate damage"),
  DEATH_PRISONER("Death of prisoner"),
  DEATH_OTHER("Death of other person"),
  DIRTY_PROTEST("Dirty protest"),
  DISORDER("Disorder"),
  DRONE_SIGHTING("Drone sighting"),
  DRUGS("Drugs"),
  ESCAPE_FROM_PRISON("Escape from establishment"),
  ESCAPE_FROM_ESCORT("Escape from escort"),
  FIND("Find of illicit items"),
  FIRE("Fire"),
  FIREARM("Firearm, ammunition or chemical incapacitant"),
  FOOD_REFUSAL("Food or liquid refusal"),
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
