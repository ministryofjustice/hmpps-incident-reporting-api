package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.incidentreporting.dto.IncidentReport
import uk.gov.justice.digital.hmpps.incidentreporting.service.EventPublishAndAuditService
import uk.gov.justice.digital.hmpps.incidentreporting.service.IncidentReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.InformationSource

abstract class EventBaseResource {

  @Autowired
  private lateinit var eventPublishAndAuditService: EventPublishAndAuditService

  protected fun eventPublishAndAudit(
    event: IncidentReportDomainEventType,
    function: () -> IncidentReport,
    informationSource: InformationSource = InformationSource.DPS,
  ) =
    function().also { incidentReport ->
      eventPublishAndAuditService.publishEvent(
        eventType = event,
        incidentReport = incidentReport,
        auditData = incidentReport,
        source = informationSource,
      )
    }
}
