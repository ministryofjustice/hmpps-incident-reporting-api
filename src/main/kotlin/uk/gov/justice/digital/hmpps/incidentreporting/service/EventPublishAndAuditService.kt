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
    sendDomainEvent(eventType = eventType, reportId = reportId, source = source)

    auditData?.let {
      sendAuditEvent(
        auditType = eventType.auditType,
        id = reportId.toString(),
        auditData = it,
      )
    }
  }

  private fun sendDomainEvent(
    eventType: ReportDomainEventType,
    reportId: UUID,
    source: InformationSource,
  ) {
    snsService.publishDomainEvent(
      eventType = eventType,
      description = "$reportId ${eventType.description}",
      occurredAt = LocalDateTime.now(clock),
      additionalInformation = AdditionalInformation(
        id = reportId,
        source = source,
      ),
    )
  }

  private fun sendAuditEvent(
    auditType: AuditType,
    id: String,
    auditData: Any,
  ) {
    auditService.sendMessage(
      auditType = auditType,
      id = id,
      details = auditData,
    )
  }
}
