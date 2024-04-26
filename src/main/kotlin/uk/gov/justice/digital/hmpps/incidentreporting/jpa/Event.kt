package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Event as EventDto

@Entity
class Event(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  /**
   * Human readable ID.
   * Matches incident report number when sourced from NOMIS.
   * Prefixed with “IE-” when sourced from DPS.
   */
  @Column(nullable = false, unique = true, length = 25)
  val eventId: String,

  val eventDateAndTime: LocalDateTime,
  val prisonId: String,

  var title: String,
  var description: String,

  @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val reports: MutableList<Report> = mutableListOf(),

  val createdDate: LocalDateTime,
  var lastModifiedDate: LocalDateTime,
  var lastModifiedBy: String,
) : Serializable {
  fun addReport(report: Report): Report {
    return reports.add(report).let { report }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Event

    return eventId == other.eventId
  }

  override fun hashCode(): Int {
    return eventId.hashCode()
  }

  fun toDto() = EventDto(
    eventId = eventId,
    prisonId = prisonId,
    eventDateAndTime = eventDateAndTime,
    title = title,
    description = description,
    createdDate = createdDate,
    lastModifiedDate = lastModifiedDate,
    lastModifiedBy = lastModifiedBy,
  )
}