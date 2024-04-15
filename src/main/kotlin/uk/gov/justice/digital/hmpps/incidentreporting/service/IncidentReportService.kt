package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CreateIncidentReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentEventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.IncidentReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentReportNumber
import uk.gov.justice.digital.hmpps.incidentreporting.resource.IncidentEventNotFoundException
import java.time.Clock
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport as IncidentReportDTO

@Service
@Transactional(readOnly = true)
class IncidentReportService(
  private val incidentReportRepository: IncidentReportRepository,
  private val eventRepository: IncidentEventRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val clock: Clock,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getIncidentReportById(id: UUID): IncidentReportDTO? {
    return incidentReportRepository.findById(id).getOrNull()?.toDto()
  }

  fun getIncidentReportByIncidentNumber(incidentNumber: String): IncidentReportDTO? {
    return incidentReportRepository.findOneByIncidentNumber(incidentNumber)?.toDto()
  }

  @Transactional
  fun createIncidentReport(incidentReportCreateRequest: CreateIncidentReportRequest): IncidentReportDTO {
    val event = if (incidentReportCreateRequest.createNewEvent) {
      incidentReportCreateRequest.toNewEvent(
        eventRepository.generateEventId(),
        createdBy = authenticationFacade.getUserOrSystemInContext(),
        clock = clock,
      )
    } else if (incidentReportCreateRequest.linkedEventId != null) {
      eventRepository.findOneByEventId(incidentReportCreateRequest.linkedEventId)
        ?: throw IncidentEventNotFoundException("Event with ID [${incidentReportCreateRequest.linkedEventId}] not found")
    } else {
      throw ValidationException("Either createNewEvent or linkedEventId must be provided")
    }

    val newIncidentReport = incidentReportCreateRequest.toNewEntity(
      incidentReportRepository.generateIncidentReportNumber(),
      createdBy = authenticationFacade.getUserOrSystemInContext(),
      clock = clock,
      event = event,
    )

    val createdIncident = incidentReportRepository.save(newIncidentReport).toDto()

    log.info("Created Incident Report [${createdIncident.id}]")
    telemetryClient.trackEvent(
      "Created Incident Report",
      mapOf(
        "id" to createdIncident.id.toString(),
        "prisonId" to createdIncident.prisonId,
      ),
      null,
    )

    return createdIncident
  }
}
