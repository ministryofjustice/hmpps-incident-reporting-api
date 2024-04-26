package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.toNewEntity
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.time.Clock
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report as ReportDTO

@Service
@Transactional
class SyncService(
  private val reportRepository: ReportRepository,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun upsert(syncRequest: NomisSyncRequest): ReportDTO {
    val report = if (syncRequest.id != null) {
      updateExistingReport(syncRequest.id, syncRequest)
    } else {
      createNewReport(syncRequest)
    }

    log.info("Synchronised Incident Report: ${report.id} (created: ${syncRequest.id == null}, updated: ${syncRequest.id != null})")
    telemetryClient.trackEvent(
      "Synchronised Incident Report",
      mapOf(
        "created" to "${syncRequest.id == null}",
        "updated" to "${syncRequest.id != null}",
        "id" to report.id.toString(),
        "prisonId" to report.prisonId,
      ),
      null,
    )
    return report
  }

  private fun updateExistingReport(reportId: UUID, syncRequest: NomisSyncRequest): ReportDTO {
    val reportToUpdate = reportRepository.findById(reportId)
      .orElseThrow { ReportNotFoundException(reportId.toString()) }

    reportToUpdate.updateWith(syncRequest.incidentReport, syncRequest.incidentReport.reportingStaff.username, clock)
    return reportToUpdate.toDto()
  }

  private fun createNewReport(syncRequest: NomisSyncRequest): ReportDTO {
    val reportToCreate = syncRequest.incidentReport.toNewEntity(clock)
    return reportRepository.save(reportToCreate).toDto()
  }
}
