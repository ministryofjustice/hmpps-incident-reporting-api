package uk.gov.justice.digital.hmpps.incidentreporting.constants

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
