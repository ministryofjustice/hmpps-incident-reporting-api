package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.validation.ValidationException

enum class StaffRole(
  val description: String,
) {
  ACTIVELY_INVOLVED("Actively Involved"),
  AUTHORISING_OFFICER("Authorising Officer"),
  CR_HEAD("C&R Head"),
  CR_LEFT_ARM("C&R Left Arm"),
  CR_LEGS("C&R Legs"),
  CR_RIGHT_ARM("C&R Right Arm"),
  CR_SUPERVISOR("C&R Supervisor"),
  DECEASED("Deceased"),
  FIRST_ON_SCENE("First on Scene"),
  HEALTHCARE("Healthcare"),
  HOSTAGE("Hostage"),
  IN_POSSESSION("In Possession"),
  NEGOTIATOR("Negotiator"),
  PRESENT_AT_SCENE("Present at Scene"),
  SUSPECTED_INVOLVEMENT("Suspected Involvement"),
  VICTIM("Victim"),
  WITNESS("Witness"),
}

fun mapStaffRole(code: String) =
  when (code) {
    "AI" -> StaffRole.ACTIVELY_INVOLVED
    "AO" -> StaffRole.AUTHORISING_OFFICER
    "CRH" -> StaffRole.CR_HEAD
    "CRS" -> StaffRole.CR_SUPERVISOR
    "CRLG" -> StaffRole.CR_LEGS
    "CRL" -> StaffRole.CR_LEFT_ARM
    "CRR" -> StaffRole.CR_RIGHT_ARM
    "DECEASED" -> StaffRole.DECEASED
    "FOS" -> StaffRole.FIRST_ON_SCENE
    "HEALTH" -> StaffRole.HEALTHCARE
    "HOST" -> StaffRole.HOSTAGE
    "INPOS" -> StaffRole.IN_POSSESSION
    "INV" -> StaffRole.ACTIVELY_INVOLVED
    "NEG" -> StaffRole.NEGOTIATOR
    "PAS" -> StaffRole.PRESENT_AT_SCENE
    "SUSIN" -> StaffRole.SUSPECTED_INVOLVEMENT
    "VICT" -> StaffRole.VICTIM
    "WIT" -> StaffRole.WITNESS
    else -> throw ValidationException("Unknown staff code: $code")
  }
