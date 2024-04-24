package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface GenericQuestion {
  val dataItem: String
  val dataItemDescription: String?

  fun addAnswer(
    itemValue: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): GenericQuestion

  fun getLocation(): IncidentLocation?
  fun attachLocation(location: IncidentLocation)

  fun getPrisonerInvolvement(): PrisonerInvolvement?
  fun attachPrisonerInvolvement(prisonerInvolvement: PrisonerInvolvement)

  fun getStaffInvolvement(): StaffInvolvement?
  fun attachStaffInvolvement(staffInvolvement: StaffInvolvement)

  fun getEvidence(): Evidence?
  fun attachEvidence(evidence: Evidence)
}
