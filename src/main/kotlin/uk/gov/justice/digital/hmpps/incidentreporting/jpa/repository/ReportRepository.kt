package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.util.UUID

@Repository
interface ReportRepository : JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {
  @EntityGraph(value = "Report.eager", type = EntityGraph.EntityGraphType.LOAD)
  fun findOneEagerlyById(id: UUID): Report?

  fun findOneByReportReference(reportReference: String): Report?

  @EntityGraph(value = "Report.eager", type = EntityGraph.EntityGraphType.LOAD)
  fun findOneEagerlyByReportReference(reportReference: String): Report?

  @Query(value = "SELECT nextval('report_sequence')", nativeQuery = true)
  fun getNextReportReference(): Long
}

fun ReportRepository.generateReportReference() = getNextReportReference().toString()
