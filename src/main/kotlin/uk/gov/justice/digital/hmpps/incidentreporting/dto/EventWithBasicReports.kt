package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Event linking multiple incident reports (including key report information)")
@JsonInclude(JsonInclude.Include.ALWAYS)
class EventWithBasicReports(
  id: UUID,
  eventReference: String,
  eventDateAndTime: LocalDateTime,
  prisonId: String,
  title: String,
  description: String,
  createdAt: LocalDateTime,
  modifiedAt: LocalDateTime,
  modifiedBy: String,

  @Schema(description = "The contained reports with key information only", required = true)
  val reports: List<ReportBasic>,
) : Event(
  id = id,
  eventReference = eventReference,
  eventDateAndTime = eventDateAndTime,
  prisonId = prisonId,
  title = title,
  description = description,
  createdAt = createdAt,
  modifiedAt = modifiedAt,
  modifiedBy = modifiedBy,
)
