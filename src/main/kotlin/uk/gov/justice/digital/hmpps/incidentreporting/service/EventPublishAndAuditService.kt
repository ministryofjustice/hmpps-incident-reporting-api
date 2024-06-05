package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class EventPublishAndAuditService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
) {

  fun publishEvent(
    eventType: ReportDomainEventType,
    reportId: UUID,
    auditData: Any? = null,
    source: InformationSource,
  ) {
    publishEvent(event = eventType, reportId = reportId, source = source)

    auditData?.let {
      auditEvent(
        auditType = eventType.auditType,
        id = reportId.toString(),
        auditData = it,
        source = source,
      )
    }
  }

  private fun publishEvent(
    event: ReportDomainEventType,
    reportId: UUID,
    source: InformationSource,
  ) {
    snsService.publishDomainEvent(
      eventType = event,
      description = "$reportId ${event.description}",
      occurredAt = LocalDateTime.now(clock),
      additionalInformation = AdditionalInformation(
        id = reportId,
        source = source,
      ),
    )
  }

  fun auditEvent(
    auditType: AuditType,
    id: String,
    auditData: Any,
    source: InformationSource,
  ) {
    auditService.sendMessage(
      auditType = auditType,
      id = id,
      details = auditData,
    )
  }
}
