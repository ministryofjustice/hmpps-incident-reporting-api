package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.util.UUID

@Repository
interface ReportRepository : JpaRepository<Report, UUID> {
  fun findOneByIncidentNumber(incidentNumber: String): Report?

  @Query(value = "SELECT nextval('report_sequence')", nativeQuery = true)
  fun getNextIncidentNumber(): Long
}

fun ReportRepository.generateIncidentNumber() = "IR-%016d".format(getNextIncidentNumber())
