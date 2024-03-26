package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentReport
import java.util.*

@Repository
interface IncidentReportRepository : JpaRepository<IncidentReport, UUID> {
  fun findOneByIncidentNumber(incidentNumber: String): IncidentReport?

  @Query(value = "SELECT nextval('incident_number_sequence')", nativeQuery = true)
  fun getNextIncidentNumber(): Long
}

fun IncidentReportRepository.generateIncidentReportNumber() = "IR-%016d".format(getNextIncidentNumber())
