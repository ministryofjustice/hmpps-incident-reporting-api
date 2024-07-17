package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Event as EventDto

@Entity
class Event(
  /**
   * Internal ID which should not be seen by users
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

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
    eventReference = eventReference,
    prisonId = prisonId,
    eventDateAndTime = eventDateAndTime,
    title = title,
    description = description,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
  )
}
