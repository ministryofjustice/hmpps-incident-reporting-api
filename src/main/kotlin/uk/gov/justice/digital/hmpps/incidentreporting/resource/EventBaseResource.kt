package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Report
import uk.gov.justice.digital.hmpps.incidentreporting.service.EventPublishAndAuditService
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import java.util.*

abstract class EventBaseResource {

  @Autowired
  private lateinit var eventPublishAndAuditService: EventPublishAndAuditService

  protected fun eventPublishAndAudit(
    event: ReportDomainEventType,
    function: () -> Report,
    informationSource: InformationSource,
  ) =
    function().also { report ->
      eventPublishAndAuditService.publishEvent(
        eventType = event,
        reportId = report.id,
        auditData = report,
        source = informationSource,
      )
    }
}
