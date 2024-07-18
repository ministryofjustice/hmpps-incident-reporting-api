package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedEntityGraphs
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.id.GeneratedUuidV7
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Event as EventDto
import uk.gov.justice.digital.hmpps.incidentreporting.dto.EventWithBasicReports as EventWithReportsDto

@Entity
@NamedEntityGraphs(
  value = [
    NamedEntityGraph(
      name = "Event.eager",
      attributeNodes = [
        NamedAttributeNode("reports"),
      ],
    ),
  ],
)
class Event(
  /**
   * Internal ID which should not be seen by users
   */
  @Id
  @GeneratedUuidV7
  @Column(name = "id", updatable = false, nullable = false)
  val id: UUID? = null,

  /**
   * Human-readable reference.
   * Matches incident number when sourced from NOMIS.
   * Prefixed with “IE-” when sourced from DPS.
   */
  @Column(nullable = false, unique = true, length = 25)
  val eventReference: String,

  var eventDateAndTime: LocalDateTime,
  var prisonId: String,

  var title: String,
  var description: String,

  @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  val reports: MutableList<Report> = mutableListOf(),

  var createdAt: LocalDateTime,
  var modifiedAt: LocalDateTime,
  var modifiedBy: String,
) {
  override fun toString(): String {
    return "Event(eventReference=$eventReference)"
  }

  fun addReport(report: Report): Report {
    return reports.add(report).let {
      report.event = this
      report
    }
  }

  fun toDto() = EventDto(
    id = id!!,
    eventReference = eventReference,
    prisonId = prisonId,
    eventDateAndTime = eventDateAndTime,
    title = title,
    description = description,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
  )

  fun toDtoWithBasicReports() = EventWithReportsDto(
    id = id!!,
    eventReference = eventReference,
    prisonId = prisonId,
    eventDateAndTime = eventDateAndTime,
    title = title,
    description = description,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    reports = reports.map { it.toDtoBasic() },
  )
}
