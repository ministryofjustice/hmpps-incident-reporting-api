package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.toNewEntity
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.time.Clock
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report as ReportDto

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

  fun upsert(syncRequest: NomisSyncRequest): ReportDto {
    val report = if (syncRequest.id != null) {
      if (syncRequest.initialMigration) {
        throw ValidationException("Cannot update an existing report (${syncRequest.id}) during initial migration")
      }
      updateExistingReport(syncRequest.id, syncRequest.incidentReport)
    } else {
      createNewReport(syncRequest.incidentReport)
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

  private fun updateExistingReport(reportId: UUID, incidentReport: NomisReport): ReportDto {
    val reportToUpdate = reportRepository.findById(reportId)
      .orElseThrow { ReportNotFoundException(reportId) }
    reportToUpdate.updateWith(incidentReport, clock)
    return reportToUpdate.toDto()
  }

  private fun createNewReport(incidentReport: NomisReport): ReportDto {
    val reportToCreate = incidentReport.toNewEntity()
    return reportRepository.save(reportToCreate).toDto()
  }
}
