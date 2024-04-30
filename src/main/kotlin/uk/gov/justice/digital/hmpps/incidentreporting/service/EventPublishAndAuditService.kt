package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report as ReportDto

@Service
class EventPublishAndAuditService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
) {

  fun publishEvent(
    eventType: ReportDomainEventType,
    reports: List<ReportDto>,
    auditData: Any? = null,
    source: InformationSource = InformationSource.DPS,
  ) {
    reports.forEach {
      publishEvent(
        eventType = eventType,
        report = it,
        auditData = it,
        source = source,
      )
    }
  }

  fun publishEvent(
    eventType: ReportDomainEventType,
    report: ReportDto,
    auditData: Any? = null,
    source: InformationSource = InformationSource.DPS,
  ) {
    publishEvent(event = eventType, report = report, source = source)

    auditData?.let {
      auditEvent(
        auditType = eventType.auditType,
        id = report.id.toString(),
        auditData = it,
        source = source,
      )
    }
  }

  private fun publishEvent(
    event: ReportDomainEventType,
    report: ReportDto,
    source: InformationSource,
  ) {
    snsService.publishDomainEvent(
      eventType = event,
      description = "${report.id} ${event.description}",
      occurredAt = LocalDateTime.now(clock),
      additionalInformation = AdditionalInformation(
        id = report.id,
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
