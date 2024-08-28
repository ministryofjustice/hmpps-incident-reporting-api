package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.StaffInvolvement

@Repository
interface StaffInvolvementRepository : JpaRepository<StaffInvolvement, Long> {
  fun findAllByStaffUsername(staffUsername: String): List<StaffInvolvement>
}
