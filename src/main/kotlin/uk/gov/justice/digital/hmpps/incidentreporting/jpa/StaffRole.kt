package uk.gov.justice.digital.hmpps.incidentreporting.jpa

enum class StaffRole(
  val description: String,
) {
  ACTIVELY_INVOLVED("Actively Involved"),
  AUTH_OFFICER("Authorising Officer"),
  HEAD("C&R Head"),
  LEFT_ARM("C&R Left Arm"),
  LEGS("C&R Legs"),
  RIGHT_ARM("C&R Right Arm"),
  SUPER("C&R Supervisor"),
  DECEASED("Deceased"),
  FOS("First on Scene"),
  HEALTHCARE("Healthcare"),
  HOSTAGE("Hostage"),
  POSSESSION("In Possession"),
  NEG("Negotiator"),
  PRESENT("Present at Scene"),
  SUS("Suspected Involvement"),
  VICTIM("Victim"),
  WITNESS("Witness"),
}
