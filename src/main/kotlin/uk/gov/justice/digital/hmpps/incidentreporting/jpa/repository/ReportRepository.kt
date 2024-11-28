package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import java.util.UUID

@Repository
interface ReportRepository : JpaRepository<Report, UUID>, JpaSpecificationExecutor<Report> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")])
  @EntityGraph(value = "Report.eager", type = EntityGraph.EntityGraphType.FETCH)
  fun findOneEagerlyById(id: UUID): Report?

  fun findOneByReportReference(reportReference: String): Report?

  @EntityGraph(value = "Report.eager", type = EntityGraph.EntityGraphType.FETCH)
  fun findOneEagerlyByReportReference(reportReference: String): Report?

  @Query(value = "SELECT nextval('report_sequence')", nativeQuery = true)
  fun getNextReportReference(): Long
}

fun ReportRepository.generateReportReference() = getNextReportReference().toString()
