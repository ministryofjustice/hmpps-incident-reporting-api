package uk.gov.justice.digital.hmpps.incidentreporting.constants

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
