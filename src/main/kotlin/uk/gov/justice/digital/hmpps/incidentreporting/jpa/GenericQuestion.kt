package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface GenericQuestion {
  val code: String
  val question: String? // TODO: should we force this to be non-null?

  fun addResponse(
    response: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): GenericQuestion

  fun getLocation(): Location?
  fun attachLocation(location: Location): GenericQuestion

  fun getPrisonerInvolvement(): PrisonerInvolvement?
  fun attachPrisonerInvolvement(prisonerInvolvement: PrisonerInvolvement): GenericQuestion

  fun getStaffInvolvement(): StaffInvolvement?
  fun attachStaffInvolvement(staffInvolvement: StaffInvolvement): GenericQuestion

  fun getEvidence(): Evidence?
  fun attachEvidence(evidence: Evidence): GenericQuestion
}
