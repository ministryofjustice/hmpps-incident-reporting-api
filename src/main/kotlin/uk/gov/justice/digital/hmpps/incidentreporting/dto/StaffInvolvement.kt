package uk.gov.justice.digital.hmpps.incidentreporting.dto

import uk.gov.justice.digital.hmpps.incidentreporting.constants.StaffRole

data class StaffInvolvement(
  val staffUsername: String,
  val staffRole: StaffRole,
  val comment: String? = null,
)
