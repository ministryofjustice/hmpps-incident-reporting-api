package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class StaffRole(
  val description: String,
) {
  ACTIVELY_INVOLVED("Actively involved"),
  AUTHORISING_OFFICER("Authorising officer"),
  CR_HEAD("Control and restraint - head"),
  CR_LEFT_ARM("Control and restraint - left arm"),
  CR_LEGS("Control and restraint - legs"),
  CR_RIGHT_ARM("Control and restraint - right arm"),
  CR_SUPERVISOR("Control and restraint - supervisor"),
  DECEASED("Deceased"),
  FIRST_ON_SCENE("First on scene"),
  HEALTHCARE("Healthcare"),
  HOSTAGE("Hostage"),
  IN_POSSESSION("In possession"),
  NEGOTIATOR("Negotiator"),
  PRESENT_AT_SCENE("Present at scene"),
  SUSPECTED_INVOLVEMENT("Suspected involvement"),
  VICTIM("Victim"),
  WITNESS("Witness"),
  ;

  companion object {
    fun fromNomisCode(role: String): StaffRole = when (role) {
      "AI" -> ACTIVELY_INVOLVED
      "AO" -> AUTHORISING_OFFICER
      "CRH" -> CR_HEAD
      "CRL" -> CR_LEFT_ARM
      "CRLG" -> CR_LEGS
      "CRR" -> CR_RIGHT_ARM
      "CRS" -> CR_SUPERVISOR
      "DEC" -> DECEASED
      "FOS" -> FIRST_ON_SCENE
      "HEALTH" -> HEALTHCARE
      "HOST" -> HOSTAGE
      "INPOS" -> IN_POSSESSION
      "INV" -> ACTIVELY_INVOLVED // TODO: needs checking, might not exist in NOMIS
      "NEG" -> NEGOTIATOR
      "PAS" -> PRESENT_AT_SCENE
      "SUSIN" -> SUSPECTED_INVOLVEMENT
      "VICT" -> VICTIM
      "WIT" -> WITNESS
      else -> throw ValidationException("Unknown NOMIS staff role: $role")
    }
  }
}
