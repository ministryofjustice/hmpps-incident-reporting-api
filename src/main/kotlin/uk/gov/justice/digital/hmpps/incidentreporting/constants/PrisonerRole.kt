package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/**
 * The role of a prisoner in an incident.
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class PrisonerRole(
  val description: String,
  val nomisCode: String,
) {
  ABSCONDER("Absconder", "ABS"),
  ACTIVE_INVOLVEMENT("Active involvement", "ACTINV"),
  ASSAILANT("Assailant", "ASSIAL"),
  ASSISTED_STAFF("Assisted staff", "ASSIST"),
  DECEASED("Deceased", "DEC"),
  ESCAPE("Escapee", "ESC"),
  FIGHTER("Fighter", "FIGHT"),
  HOSTAGE("Hostage", "HOST"),
  IMPEDED_STAFF("Impeded staff", "IMPED"),
  IN_POSSESSION("In possession", "INPOSS"),
  INTENDED_RECIPIENT("Intended recipient", "INREC"),
  LICENSE_FAILURE("License failure", "LICFAIL"),
  PERPETRATOR("Perpetrator", "PERP"),
  PRESENT_AT_SCENE("Present at scene", "PRESENT"),
  SUSPECTED_ASSAILANT("Suspected assailant", "SUSASS"),
  SUSPECTED_INVOLVED("Suspected involved", "SUSINV"),
  TEMPORARY_RELEASE_FAILURE("Temporary release failure", "TRF"),
  VICTIM("Victim", "VICT"),
  ;

  companion object {
    fun fromNomisCode(code: String): PrisonerRole = entries.find { it.nomisCode == code }
      ?: throw ValidationException("Unknown NOMIS prisoner role code: $code")
  }
}
