package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report
import uk.gov.justice.digital.hmpps.incidentreporting.service.EventPublishAndAuditService
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType

abstract class EventBaseResource {

  @Autowired
  private lateinit var eventPublishAndAuditService: EventPublishAndAuditService

  protected fun eventPublishAndAudit(
    event: ReportDomainEventType,
    function: () -> Report,
    informationSource: InformationSource = InformationSource.DPS,
  ) =
    function().also { report ->
      eventPublishAndAuditService.publishEvent(
        eventType = event,
        report = report,
        auditData = report,
        source = informationSource,
      )
    }
}
