package uk.gov.justice.digital.hmpps.incidentreporting.resource

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.dto.ReportBasic
import uk.gov.justice.digital.hmpps.incidentreporting.service.AdditionalInformation
import uk.gov.justice.digital.hmpps.incidentreporting.service.EventPublishAndAuditService
import uk.gov.justice.digital.hmpps.incidentreporting.service.ReportDomainEventType
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged

abstract class EventBaseResource {

  @Autowired
  private lateinit var eventPublishAndAuditService: EventPublishAndAuditService

  protected fun <T : ReportBasic> eventPublishAndAudit(
    event: ReportDomainEventType,
    informationSource: InformationSource,
    whatChanged: WhatChanged,
    block: () -> T,
  ): T = block().also { report ->
    eventPublishAndAuditService.publishEvent(
      eventType = event,
      additionalInformation = AdditionalInformation(
        id = report.id,
        reportReference = report.reportReference,
        source = informationSource,
        location = report.location,
        whatChanged = whatChanged,
      ),
      auditData = report,
    )
  }
}
