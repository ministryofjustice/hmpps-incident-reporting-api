package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface IncidentReportRepository : JpaRepository<IncidentReport, UUID> {
  fun findOneByIncidentNumber(incidentNumber: String): IncidentReport?
}
