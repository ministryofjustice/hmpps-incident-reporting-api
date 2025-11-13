package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SnsService(
  hmppsQueueService: HmppsQueueService,
  private val zoneId: ZoneId,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }

  @WithSpan(value = "hmpps-domain-events-topic", kind = SpanKind.PRODUCER)
  fun publishDomainEvent(
    eventType: ReportDomainEventType,
    description: String,
    occurredAt: LocalDateTime,
    additionalInformation: AdditionalInformation? = null,
  ) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        eventType = eventType.value,
        additionalInformation = additionalInformation,
        occurredAt = occurredAt.atZone(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        description = description,
      ),
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation)
    domaineventsTopic.publish(
      eventType = payload.eventType.toString(),
      event = objectMapper.writeValueAsString(payload),
    ).also { log.info("Published event $payload to outbound topic") }
  }
}

@Suppress("ktlint:standard:spacing-between-declarations-with-comments")
enum class WhatChanged {
  /** Anything in the report potentially changed */
  ANYTHING,
  /** Changes to a report’s basic information */
  BASIC_REPORT,
  /** Report type changed */
  TYPE,
  /** Report status changed */
  STATUS,

  /** Added, updated or deleted a description addendum */
  DESCRIPTION_ADDENDUMS,
  /** Added, updated or deleted an involved prisoner */
  PRISONERS_INVOLVED,
  /** Added, updated or deleted an involved member of staff */
  STAFF_INVOLVED,
  /** Added, updated or deleted a correction request */
  CORRECTION_REQUESTS,

  /** Added or deleted a question with responses */
  QUESTIONS,
}

data class AdditionalInformation(
  /** Internal ID */
  val id: UUID,
  /** Human-readable reference */
  val reportReference: String,
  val source: InformationSource,
  val location: String,
  val whatChanged: WhatChanged,
)

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation?,
  val version: Int = 1,
  val occurredAt: String,
  val description: String,
)

enum class ReportDomainEventType(
  val value: String,
  val description: String,
  val auditType: AuditType,
) {
  INCIDENT_REPORT_CREATED(
    "incident.report.created",
    "An incident report has been created",
    AuditType.INCIDENT_REPORT_CREATED,
  ),
  INCIDENT_REPORT_AMENDED(
    "incident.report.amended",
    "An incident report has been amended",
    AuditType.INCIDENT_REPORT_AMENDED,
  ),
  INCIDENT_REPORT_DELETED(
    "incident.report.deleted",
    "An incident report has been deleted",
    AuditType.INCIDENT_REPORT_DELETED,
  ),
}
