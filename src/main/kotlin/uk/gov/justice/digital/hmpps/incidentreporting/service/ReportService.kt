package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.incidentreporting.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.incidentreporting.config.trackEvent
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportWithDetails
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.CreateReportRequest
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.EventRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.ReportRepository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateEventId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository.generateIncidentNumber
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByIncidentDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByPrisonId
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateFrom
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByReportedDateUntil
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterBySource
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByStatuses
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications.filterByType
import uk.gov.justice.digital.hmpps.incidentreporting.resource.EventNotFoundException
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

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

  fun getReports(
    prisonId: String? = null,
    source: InformationSource? = null,
    statuses: List<Status> = emptyList(),
    type: Type? = null,
    incidentDateFrom: LocalDate? = null,
    incidentDateUntil: LocalDate? = null,
    reportedDateFrom: LocalDate? = null,
    reportedDateUntil: LocalDate? = null,
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("incidentDateAndTime").descending()),
  ): Page<ReportWithDetails> {
    val specification = Specification.allOf(
      buildList {
        prisonId?.let { add(filterByPrisonId(prisonId)) }
        source?.let { add(filterBySource(source)) }
        if (statuses.isNotEmpty()) {
          add(filterByStatuses(statuses))
        }
        type?.let { add(filterByType(type)) }
        incidentDateFrom?.let { add(filterByIncidentDateFrom(incidentDateFrom)) }
        incidentDateUntil?.let { add(filterByIncidentDateUntil(incidentDateUntil)) }
        reportedDateFrom?.let { add(filterByReportedDateFrom(reportedDateFrom)) }
        reportedDateUntil?.let { add(filterByReportedDateUntil(reportedDateUntil)) }
      },
    )
    return reportRepository.findAll(specification, pageable)
      .map { it.toDtoWithDetails() }
  }

  fun getReportById(id: UUID): ReportWithDetails? {
    return reportRepository.findOneEagerlyById(id)
      ?.toDtoWithDetails()
  }

  fun getReportByIncidentNumber(incidentNumber: String): ReportWithDetails? {
    return reportRepository.findOneEagerlyByIncidentNumber(incidentNumber)
      ?.toDtoWithDetails()
  }

  @Transactional
  fun deleteReportById(id: UUID, deleteOrphanedEvents: Boolean = true): ReportWithDetails? {
    return reportRepository.findOneEagerlyById(id)?.let { report ->
      val eventIdToDelete = if (deleteOrphanedEvents && report.event.reports.size == 1) {
        report.event.id!!
      } else {
        null
      }
      report.toDtoWithDetails().also {
        report.event.reports.removeIf { it.id == id }
        reportRepository.deleteById(id)

        log.info("Deleted incident report number=${report.incidentNumber} ID=${report.id}")
        telemetryClient.trackEvent(
          "Deleted incident report",
          it,
        )

        eventIdToDelete?.let { eventId ->
          eventRepository.deleteById(eventId)

          log.info("Deleted event ID=$eventId")
        }
      }
    }
  }

  @Transactional
  fun createReport(createReportRequest: CreateReportRequest): ReportWithDetails {
    createReportRequest.validate()

    val event = if (createReportRequest.linkedEventId != null) {
      eventRepository.findOneByEventId(createReportRequest.linkedEventId)
        ?: throw EventNotFoundException(createReportRequest.linkedEventId)
    } else {
      createReportRequest.toNewEvent(
        eventRepository.generateEventId(),
        createdBy = authenticationFacade.getUserOrSystemInContext(),
        clock = clock,
      )
    }

    val newReport = createReportRequest.toNewEntity(
      incidentNumber = reportRepository.generateIncidentNumber(),
      createdBy = authenticationFacade.getUserOrSystemInContext(),
      clock = clock,
      event = event,
    )

    val report = reportRepository.save(newReport).toDtoWithDetails()

    log.info("Created incident report number=${report.incidentNumber} ID=${report.id}")
    telemetryClient.trackEvent(
      "Created incident report",
      report,
    )

    return report
  }
}
