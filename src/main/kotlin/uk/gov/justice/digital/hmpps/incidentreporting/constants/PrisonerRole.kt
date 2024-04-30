package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class PrisonerRole(
  val description: String,
) {
  VICTIM("Victim"),
  PERPETRATOR("Perpetrator"),
  ABSCONDER("Absconder"),
  ACTIVE_INVOLVEMENT("Active involvement"),
  ASSAILANT("Assailant"),
  ASSISTED_STAFF("Assisted staff"),
  DECEASED("Deceased"),
  ESCAPE("Escapee"),
  FIGHTER("Fighter"),
  HOSTAGE("Hostage"),
  IMPEDED_STAFF("Impeded staff"),
  IN_POSSESSION("In possession"),
  INTENDED_RECIPIENT("Intended recipient"),
  LICENSE_FAILURE("License failure"),
  PRESENT_AT_SCENE("Present at scene"),
  SUSPECTED_ASSAILANT("Suspected assailant"),
  SUSPECTED_INVOLVED("Suspected involved"),
  TEMPORARY_RELEASE_FAILURE("Temporary release failure"),
  ;

  companion object {
    fun fromNomisCode(role: String): PrisonerRole = when (role) {
      "ABSCONDEE" -> ABSCONDER
      "ACTIVE_INVOLVEMENT" -> ACTIVE_INVOLVEMENT
      "ASSAILANT" -> ASSAILANT
      "ASSISTED_STAFF" -> ASSISTED_STAFF
      "DECEASED" -> DECEASED
      "ESCAPE" -> ESCAPE
      "FIGHT" -> FIGHTER
      "HOST" -> HOSTAGE
      "IMPED" -> IMPEDED_STAFF
      "INPOSS" -> IN_POSSESSION
      "INREC" -> INTENDED_RECIPIENT
      "LICFAIL" -> LICENSE_FAILURE
      "PERP" -> PERPETRATOR
      "PRESENT" -> PRESENT_AT_SCENE
      "SUSASS" -> SUSPECTED_ASSAILANT
      "SUSINV" -> SUSPECTED_INVOLVED
      "TRF" -> TEMPORARY_RELEASE_FAILURE
      "VICT" -> VICTIM
      else -> throw ValidationException("Unknown NOMIS prisoner role: $role")
    }
  }
}
