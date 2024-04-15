package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.PrisonerInvolvement

@Repository
interface PrisonerInvolvementRepository : JpaRepository<PrisonerInvolvement, Long> {
  fun findAllByPrisonerNumber(prisonerNumber: String): List<PrisonerInvolvement>
}
