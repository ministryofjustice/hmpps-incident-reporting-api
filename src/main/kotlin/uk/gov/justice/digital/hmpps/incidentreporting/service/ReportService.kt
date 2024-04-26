package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentNumber
import uk.gov.justice.digital.hmpps.incidentreporting.resource.EventNotFoundException
import java.time.Clock
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report as ReportDto

@Service
@Transactional(readOnly = true)
class ReportService(
  private val reportRepository: ReportRepository,
  private val eventRepository: EventRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val clock: Clock,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getReportById(id: UUID): ReportDto? {
    return reportRepository.findById(id).getOrNull()?.toDto()
  }

  fun getReportByIncidentNumber(incidentNumber: String): ReportDto? {
    return reportRepository.findOneByIncidentNumber(incidentNumber)?.toDto()
  }

  @Transactional
  fun createReport(createReportRequest: CreateReportRequest): ReportDto {
    val event = if (createReportRequest.createNewEvent) {
      createReportRequest.toNewEvent(
        eventRepository.generateEventId(),
        createdBy = authenticationFacade.getUserOrSystemInContext(),
        clock = clock,
      )
    } else if (createReportRequest.linkedEventId != null) {
      eventRepository.findOneByEventId(createReportRequest.linkedEventId)
        ?: throw EventNotFoundException("Event with ID [${createReportRequest.linkedEventId}] not found")
    } else {
      throw ValidationException("Either createNewEvent or linkedEventId must be provided")
    }

    val newReport = createReportRequest.toNewEntity(
      reportRepository.generateIncidentNumber(),
      createdBy = authenticationFacade.getUserOrSystemInContext(),
      clock = clock,
      event = event,
    )

    val report = reportRepository.save(newReport).toDto()

    log.info("Created incident report [${report.id}]")
    telemetryClient.trackEvent(
      "Created incident report",
      mapOf(
        "id" to report.id.toString(),
        "prisonId" to report.prisonId,
      ),
      null,
    )

    return report
  }
}
