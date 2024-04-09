package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.model.nomis.toNewEntity
import uk.gov.justice.digital.hmpps.incidentreporting.resource.IncidentReportNotFoundException
import uk.gov.justice.digital.hmpps.incidentreporting.resource.UpsertNomisIncident
import java.time.Clock
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport as IncidentDTO

@Service
@Transactional
class SyncService(
  private val incidentReportRepository: IncidentReportRepository,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun upsert(upsert: UpsertNomisIncident): IncidentDTO {
    val incident = if (upsert.id != null) {
      updateIncident(upsert)
    } else {
      createIncident(upsert)
    }

    log.info("Synchronised Incident Report: ${incident.id} (created: ${upsert.id == null}, updated: ${upsert.id != null})")
    telemetryClient.trackEvent(
      "Synchronised Incident Report",
      mapOf(
        "created" to "${upsert.id == null}",
        "updated" to "${upsert.id != null}",
        "id" to incident.id.toString(),
        "prisonId" to incident.prisonId,
      ),
      null,
    )
    return incident
  }

  private fun updateIncident(upsert: UpsertNomisIncident): IncidentDTO {
    val incidentToUpdate = incidentReportRepository.findById(upsert.id!!)
      .orElseThrow { IncidentReportNotFoundException(upsert.toString()) }

    incidentToUpdate.updateWith(upsert.incidentReport, upsert.incidentReport.reportingStaff.username, clock)
    return incidentToUpdate.toDto()
  }

  private fun createIncident(upsert: UpsertNomisIncident): IncidentDTO {
    val incidentToCreate = upsert.incidentReport.toNewEntity(clock)
    return incidentReportRepository.save(incidentToCreate).toDto()
  }
}
