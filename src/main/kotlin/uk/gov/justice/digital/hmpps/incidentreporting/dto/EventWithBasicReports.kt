package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "Event linking multiple incident reports (including key report information)",
  accessMode = Schema.AccessMode.READ_ONLY,
)
@JsonInclude(JsonInclude.Include.ALWAYS)
class EventWithBasicReports(
  id: UUID,
  eventReference: String,
  eventDateAndTime: LocalDateTime,
  location: String,
  title: String,
  description: String,
  createdAt: LocalDateTime,
  modifiedAt: LocalDateTime,
  modifiedBy: String,

  @Schema(description = "The contained reports with key information only")
  val reports: List<ReportBasic>,
) : Event(
  id = id,
  eventReference = eventReference,
  eventDateAndTime = eventDateAndTime,
  location = location,
  title = title,
  description = description,
  createdAt = createdAt,
  modifiedAt = modifiedAt,
  modifiedBy = modifiedBy,
)
