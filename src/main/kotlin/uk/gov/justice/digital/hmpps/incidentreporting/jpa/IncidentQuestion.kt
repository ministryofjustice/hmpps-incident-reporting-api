package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import java.time.LocalDateTime

interface IncidentQuestion {
  val dataItem: String
  val dataItemDescription: String?

  fun addAnswer(
    itemValue: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): IncidentQuestion

  fun attachLocation(location: IncidentLocation)
  fun attachPrisonerInvolvement(prisonerInvolvement: PrisonerInvolvement)
  fun attachEvidence(evidence: Evidence)
  fun attachStaffInvolvement(staffInvolvement: StaffInvolvement)

  fun getEvidence(): Evidence?
  fun getStaffInvolvement(): StaffInvolvement?
  fun getPrisonerInvolvement(): PrisonerInvolvement?
  fun getLocation(): IncidentLocation?
}
