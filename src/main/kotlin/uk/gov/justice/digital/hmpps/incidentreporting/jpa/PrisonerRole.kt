package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.validation.ValidationException

enum class PrisonerRole(
  val description: String,
) {
  VICTIM("Victim"),
  PERPETRATOR("Perpetrator"),
  ABSCONDER("Absconder"),
  ACTIVE_INVOLVEMENT("Active Involvement"),
  ASSAILANT("Assailant"),
  ASSISTED_STAFF("Assisted Staff"),
  DECEASED("Deceased"),
  ESCAPE("Escapee"),
  FIGHTER("Fighter"),
  HOSTAGE("Hostage"),
  IMPEDED_STAFF("Impeded Staff"),
  IN_POSSESSION("In Possession"),
  INTENDED_RECIPIENT("Intended Recipient"),
  LICENSE_FAILURE("License Failure"),
  PRESENT_AT_SCENE("Present at scene"),
  SUSPECTED_ASSAILANT("Suspected Assailant"),
  SUSPECTED_INVOLVED("Suspected Involved"),
  TEMPORARY_RELEASE_FAILURE("Temporary Release Failure"),
}

fun mapPrisonerRole(code: String): PrisonerRole = when (code) {
  "ABSCONDEE" -> PrisonerRole.ABSCONDER
  "ACTIVE_INVOLVEMENT" -> PrisonerRole.ACTIVE_INVOLVEMENT
  "ASSAILANT" -> PrisonerRole.ASSAILANT
  "ASSISTED_STAFF" -> PrisonerRole.ASSISTED_STAFF
  "DECEASED" -> PrisonerRole.DECEASED
  "ESCAPE" -> PrisonerRole.ESCAPE
  "FIGHT" -> PrisonerRole.FIGHTER
  "HOST" -> PrisonerRole.HOSTAGE
  "IMPED" -> PrisonerRole.IMPEDED_STAFF
  "INPOSS" -> PrisonerRole.IN_POSSESSION
  "INREC" -> PrisonerRole.INTENDED_RECIPIENT
  "LICFAIL" -> PrisonerRole.LICENSE_FAILURE
  "PERP" -> PrisonerRole.PERPETRATOR
  "PRESENT" -> PrisonerRole.PRESENT_AT_SCENE
  "SUSASS" -> PrisonerRole.SUSPECTED_ASSAILANT
  "SUSINV" -> PrisonerRole.SUSPECTED_INVOLVED
  "TRF" -> PrisonerRole.TEMPORARY_RELEASE_FAILURE
  "VICT" -> PrisonerRole.VICTIM
  else -> throw ValidationException("Unknown prisoner role: $code")
}
