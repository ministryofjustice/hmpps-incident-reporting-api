package uk.gov.justice.digital.hmpps.incidentreporting.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Event linking multiple incident reports")
data class Event(
  val eventId: String,
  val eventDateAndTime: LocalDateTime,
  val prisonId: String,

  val title: String,
  val description: String,

  val createdDate: LocalDateTime,
  var lastModifiedDate: LocalDateTime,
  var lastModifiedBy: String,
)
