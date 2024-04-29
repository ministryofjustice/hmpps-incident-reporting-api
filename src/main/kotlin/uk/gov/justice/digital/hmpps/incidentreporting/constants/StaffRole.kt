package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class StaffRole(
  val description: String,
) {
  ACTIVELY_INVOLVED("Actively involved"),
  AUTHORISING_OFFICER("Authorising officer"),
  CR_HEAD("C&R Head"),
  CR_LEFT_ARM("C&R Left Arm"),
  CR_LEGS("C&R Legs"),
  CR_RIGHT_ARM("C&R Right Arm"),
  CR_SUPERVISOR("C&R Supervisor"),
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
      "CRS" -> CR_SUPERVISOR
      "CRLG" -> CR_LEGS
      "CRL" -> CR_LEFT_ARM
      "CRR" -> CR_RIGHT_ARM
      "DECEASED" -> DECEASED
      "FOS" -> FIRST_ON_SCENE
      "HEALTH" -> HEALTHCARE
      "HOST" -> HOSTAGE
      "INPOS" -> IN_POSSESSION
      "INV" -> ACTIVELY_INVOLVED
      "NEG" -> NEGOTIATOR
      "PAS" -> PRESENT_AT_SCENE
      "SUSIN" -> SUSPECTED_INVOLVEMENT
      "VICT" -> VICTIM
      "WIT" -> WITNESS
      else -> throw ValidationException("Unknown NOMIS staff role: $role")
    }
  }
}
