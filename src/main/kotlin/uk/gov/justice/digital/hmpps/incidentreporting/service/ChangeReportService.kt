package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Service
class ChangeReportService(
  private val reportRepository: ReportRepository,
) {
  @Transactional(readOnly = true)
  fun findReportOrThrowNotFound(reportId: UUID): Report {
    return reportRepository.findById(reportId).getOrNull()
      ?: throw ReportNotFoundException(reportId)
  }

  @Transactional
  fun changeReportOrThrowNotFound(
    reportId: UUID,
    now: LocalDateTime,
    requestUser: String,
    changeReport: (Report) -> Unit,
  ): Report {
    return findReportOrThrowNotFound(reportId)
      .also(changeReport)
      .also { report ->
        report.modifiedIn = InformationSource.DPS
        report.modifiedAt = now
        report.modifiedBy = requestUser
      }
  }
}
