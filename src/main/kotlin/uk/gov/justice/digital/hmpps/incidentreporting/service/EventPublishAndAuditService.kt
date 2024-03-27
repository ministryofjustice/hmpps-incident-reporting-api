package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport as IncidentReportDTO

@Service
class EventPublishAndAuditService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
) {

  fun publishEvent(
    eventType: IncidentReportDomainEventType,
    incidentReport: List<IncidentReportDTO>,
    auditData: Any? = null,
    source: InformationSource = InformationSource.DPS,
  ) {
    incidentReport.forEach {
      publishEvent(eventType = eventType, incidentReport = it, auditData = it, source = source)
    }
  }

  fun publishEvent(
    eventType: IncidentReportDomainEventType,
    incidentReport: IncidentReportDTO,
    auditData: Any? = null,
    source: InformationSource = InformationSource.DPS,
  ) {
    publishEvent(event = eventType, incidentReport = incidentReport, source = source)

    auditData?.let {
      auditEvent(
        auditType = eventType.auditType,
        id = incidentReport.id.toString(),
        auditData = it,
        source = source,
      )
    }
  }

  private fun publishEvent(
    event: IncidentReportDomainEventType,
    incidentReport: IncidentReportDTO,
    source: InformationSource,
  ) {
    snsService.publishDomainEvent(
      eventType = event,
      description = "${incidentReport.id} ${event.description}",
      occurredAt = LocalDateTime.now(clock),
      additionalInformation = AdditionalInformation(
        id = incidentReport.id,
        source = source,
      ),
    )
  }

  fun auditEvent(
    auditType: AuditType,
    id: String,
    auditData: Any,
    source: InformationSource = InformationSource.DPS,
  ) {
    auditService.sendMessage(
      auditType = auditType,
      id = id,
      details = auditData,
    )
  }
}

enum class InformationSource {
  DPS,
  NOMIS,
}
