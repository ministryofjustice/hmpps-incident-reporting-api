package uk.gov.justice.digital.hmpps.incidentreporting.jpa

/**
 * ABS	Abscondee
 * ACTINV	Active Involvement
 * ASSIAL	Assailant
 * ASSIST	Assisted Staff
 * DEC	Deceased
 * ESC	Escapee
 * FIGHT	Fighter
 * HOST	Hostage
 * IMPED	Impeded Staff
 * INPOSS	In Possession
 * INREC	Intended Recipient
 * LICFAIL	License Failure
 * PERP	Perpetrator
 * PRESENT	Present at scene
 * SUSASS	Suspected Assailant
 * SUSINV	Suspected Involved
 * TRF	Temporary Release Failure
 * VICT	Victim
 */
enum class PrisonerRole(
  val description: String,
) {
  ABS("Abscondee"),
  ACTINV("Active Involvement"),
  ASSIAL("Assailant"),
  ASSIST("Assisted Staff"),
  DEC("Deceased"),
  ESC("Escapee"),
  FIGHT("Fighter"),
  HOST("Hostage"),
  IMPED("Impeded Staff"),
  INPOSS("In Possession"),
  INREC("Intended Recipient"),
  LICFAIL("License Failure"),
  PERP("Perpetrator"),
  PRESENT("Present at scene"),
  SUSASS("Suspected Assailant"),
  SUSINV("Suspected Involved"),
  TRF("Temporary Release Failure"),
  VICT("Victim"),
}
