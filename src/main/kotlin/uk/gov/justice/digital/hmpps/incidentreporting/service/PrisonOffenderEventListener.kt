package uk.gov.justice.digital.hmpps.incidentreporting.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrisonOffenderEventListener(
  private val reportService: ReportService,
  private val mapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    const val PRISONER_MERGE_EVENT_TYPE = "prison-offender-events.prisoner.merged"
    const val PRISONER_BOOKING_MOVED_EVENT_TYPE = "prison-offender-events.prisoner.booking.moved"
  }

  @SqsListener("incidentreporting", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-incident-reporting-prisoner-event-queue", kind = SpanKind.SERVER)
  fun onPrisonOffenderEvent(requestJson: String) {
    val (message, messageAttributes) = mapper.readValue(requestJson, HMPPSMessage::class.java)
    val eventType = messageAttributes.eventType.Value
    log.info("Received message $message, type $eventType")

    when (eventType) {
      PRISONER_MERGE_EVENT_TYPE -> {
        val mergeEvent = mapper.readValue(message, HMPPSMergeDomainEvent::class.java)
        reportService.replacePrisonerNumber(
          removedPrisonerNumber = mergeEvent.additionalInformation.removedNomsNumber,
          prisonerNumber = mergeEvent.additionalInformation.nomsNumber,
        )
      }
      PRISONER_BOOKING_MOVED_EVENT_TYPE -> {
        val mergeEvent = mapper.readValue(message, HMPPSMergeBookingMovedEvent::class.java)
        reportService.replacePrisonerNumber(
          removedPrisonerNumber = mergeEvent.additionalInformation.movedFromNomsNumber,
          prisonerNumber = mergeEvent.additionalInformation.movedToNomsNumber,
        )
      }
      else -> {
        log.debug("Ignoring message with type $eventType")
      }
    }
  }
}

data class HMPPSMergeDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformationMerge,
  val version: String,
  val occurredAt: String,
  val description: String,
)

data class AdditionalInformationMerge(
  val nomsNumber: String,
  val removedNomsNumber: String,
)

data class HMPPSMergeBookingMovedEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformationBookingMoved,
  val version: String,
  val occurredAt: String,
  val description: String,
)

data class AdditionalInformationBookingMoved(
  val bookingId: Long,
  val movedFromNomsNumber: String,
  val movedToNomsNumber: String,
)

data class HMPPSEventType(val Value: String, val Type: String)
data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
data class HMPPSMessage(
  val Message: String,
  val MessageAttributes: HMPPSMessageAttributes,
)
