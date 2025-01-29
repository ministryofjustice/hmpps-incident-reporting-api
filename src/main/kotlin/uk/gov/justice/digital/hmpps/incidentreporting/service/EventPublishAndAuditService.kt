package uk.gov.justice.digital.hmpps.incidentreporting.service

import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class EventPublishAndAuditService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
) {

  fun publishEvent(
    eventType: ReportDomainEventType,
    additionalInformation: AdditionalInformation,
    auditData: Any? = null,
  ) {
    sendDomainEvent(
      eventType = eventType,
      additionalInformation = additionalInformation,
    )

    auditData?.let {
      sendAuditEvent(
        auditType = eventType.auditType,
        id = additionalInformation.id.toString(),
        auditData = it,
      )
    }
  }

  private fun sendDomainEvent(eventType: ReportDomainEventType, additionalInformation: AdditionalInformation) {
    snsService.publishDomainEvent(
      eventType = eventType,
      description = "${additionalInformation.id} ${eventType.description}",
      occurredAt = LocalDateTime.now(clock),
      additionalInformation = additionalInformation,
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
