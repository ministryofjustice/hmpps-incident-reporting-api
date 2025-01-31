package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/**
 * The role of a staff member in an incident.
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class StaffRole(
  val description: String,
  vararg val nomisCodes: String,
) {
  // NB: AI & INV both map to ACTIVELY_INVOLVED (their descriptions match in NOMIS)
  ACTIVELY_INVOLVED("Actively involved", "AI", "INV"),
  AUTHORISING_OFFICER("Authorising officer", "AO"),
  CR_HEAD("Control and restraint - head", "CRH"),
  CR_LEFT_ARM("Control and restraint - left arm", "CRL"),
  CR_LEGS("Control and restraint - legs", "CRLG"),
  CR_RIGHT_ARM("Control and restraint - right arm", "CRR"),
  CR_SUPERVISOR("Control and restraint - supervisor", "CRS"),
  DECEASED("Deceased", "DEC"),
  FIRST_ON_SCENE("First on scene", "FOS"),
  HEALTHCARE("Healthcare", "HEALTH"),
  HOSTAGE("Hostage", "HOST"),
  IN_POSSESSION("In possession", "INPOS"),
  NEGOTIATOR("Negotiator", "NEG"),
  PRESENT_AT_SCENE("Present at scene", "PAS"),
  SUSPECTED_INVOLVEMENT("Suspected involvement", "SUSIN"),
  VICTIM("Victim", "VICT"),
  WITNESS("Witness", "WIT"),
  ;

  companion object {
    fun fromNomisCode(code: String): StaffRole = entries.find { it.nomisCodes.contains(code) }
      ?: throw ValidationException("Unknown NOMIS staff role code: $code")
  }
}
