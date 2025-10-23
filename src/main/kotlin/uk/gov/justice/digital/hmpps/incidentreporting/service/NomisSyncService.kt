package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.config.trackEvent
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncDeleteRequest
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportAlreadyExistsException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportModifiedInDpsException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.time.Clock
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report as ReportEntity

@Service
@Transactional(rollbackFor = [ReportAlreadyExistsException::class])
class NomisSyncService(
  private val reportRepository: ReportRepository,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun upsert(syncRequest: NomisSyncRequest): ReportWithDetails {
    log.info("Starting synchronisation of Incident Report: ${syncRequest.incidentReport.incidentId}")
    syncRequest.validate()

    val id = syncRequest.id
    val report = if (id != null) {
      updateExistingReport(id, syncRequest.incidentReport)
    } else {
      createNewReport(syncRequest.incidentReport)
    }

    log.info(
      "Synchronised Incident Report: ${report.id} (created: ${syncRequest.id == null}, updated: ${syncRequest.id != null})",
    )
    telemetryClient.trackEvent(
      "Synchronised Incident Report",
      report,
      mapOf(
        "created" to "${syncRequest.id == null}",
        "updated" to "${syncRequest.id != null}",
      ),
    )
    return report
  }

  fun delete(syncRequest: NomisSyncDeleteRequest): ReportWithDetails {
    val id = syncRequest.id
    log.info("Request to deletion of incident report with UUID: $id")

    val report = reportRepository.findById(id).orElseThrow { ReportNotFoundException(id) }
    val deletedReport = report.toDtoWithDetails()
    log.info("Deleting report reference: ${deletedReport.reportReference}")
    reportRepository.delete(report)

    log.info(
      "Deleted Incident Report: ${deletedReport.reportReference}",
    )
    telemetryClient.trackEvent(
      "Deleted Incident Report",
      deletedReport,
    )
    return deletedReport
  }

  private fun updateExistingReport(reportId: UUID, incidentReport: NomisReport): ReportWithDetails {
    log.info("Locking existing report: ${incidentReport.incidentId}")
    reportRepository.findReportByIdAndLockRecord(reportId) // will lock this table row.
    log.info("Lock obtained for: ${incidentReport.incidentId}")
    val reportToUpdate = reportRepository.findOneEagerlyById(reportId) ?: throw ReportNotFoundException(reportId)

    if (reportToUpdate.modifiedIn != InformationSource.NOMIS) {
      throw ReportModifiedInDpsException(reportId)
    }
    reportToUpdate.updateWith(incidentReport, clock)

    return reportToUpdate.toDtoWithDetails(includeHistory = true)
  }

  private fun createNewReport(incidentReport: NomisReport): ReportWithDetails {
    val unsavedReportEntity = ReportEntity.createReport(incidentReport)
    val reportEntity = try {
      reportRepository.save(unsavedReportEntity)
    } catch (e: DataIntegrityViolationException) {
      val constraintViolation = e.cause as? org.hibernate.exception.ConstraintViolationException
      if (
        constraintViolation != null &&
        listOf("report_reference").contains(constraintViolation.constraintName)
      ) {
        throw ReportAlreadyExistsException("${incidentReport.incidentId}")
      } else {
        throw e
      }
    }
    return reportEntity.toDtoWithDetails(includeHistory = true)
  }
}
