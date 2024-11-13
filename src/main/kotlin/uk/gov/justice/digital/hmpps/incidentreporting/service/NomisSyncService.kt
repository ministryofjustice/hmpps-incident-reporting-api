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
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.toNewEntity
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.NomisSyncRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportAlreadyExistsException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportModifiedInDpsException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ReportNotFoundException
import java.time.Clock
import java.util.UUID

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
    syncRequest.validate()

    val report = if (syncRequest.id != null) {
      updateExistingReport(syncRequest.id, syncRequest.incidentReport)
    } else {
      createNewReport(syncRequest.incidentReport)
    }

    log.info("Synchronised Incident Report: ${report.id} (created: ${syncRequest.id == null}, updated: ${syncRequest.id != null})")
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

  private fun updateExistingReport(reportId: UUID, incidentReport: NomisReport): ReportWithDetails {
    val reportToUpdate = reportRepository.findOneEagerlyById(reportId)
      ?: throw ReportNotFoundException(reportId)
    if (reportToUpdate.modifiedIn != InformationSource.NOMIS) {
      throw ReportModifiedInDpsException(reportId)
    }
    reportToUpdate.updateWith(incidentReport, clock)
    return reportToUpdate.toDtoWithDetails()
  }

  private fun createNewReport(incidentReport: NomisReport): ReportWithDetails {
    val reportToCreate = incidentReport.toNewEntity()
    val reportEntity = try {
      reportRepository.save(reportToCreate)
    } catch (e: DataIntegrityViolationException) {
      val constraintViolation = e.cause as? org.hibernate.exception.ConstraintViolationException
      if (
        constraintViolation != null &&
        listOf("event_reference", "report_reference").contains(constraintViolation.constraintName)
      ) {
        throw ReportAlreadyExistsException("${incidentReport.incidentId}")
      } else {
        throw e
      }
    }
    return reportEntity.toDtoWithDetails()
  }
}
