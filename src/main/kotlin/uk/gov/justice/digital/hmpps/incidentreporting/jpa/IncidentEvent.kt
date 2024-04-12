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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.incidentreporting.dto.EventDetail
import java.io.Serializable
import java.time.LocalDateTime

@Entity
class IncidentEvent(

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
  var eventDetails: String,

  @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  val incidents: MutableList<IncidentReport> = mutableListOf(),

  val createdDate: LocalDateTime,
  var lastModifiedDate: LocalDateTime,
  var lastModifiedBy: String,

) : Serializable {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun addIncidentReport(incidentReport: IncidentReport): IncidentReport {
    return incidents.add(incidentReport).let { incidentReport }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as IncidentEvent

    return eventId == other.eventId
  }

  override fun hashCode(): Int {
    return eventId.hashCode()
  }

  fun toDto() = EventDetail(
    eventId = eventId,
    prisonId = prisonId,
    eventDateAndTime = eventDateAndTime,
    eventDetails = eventDetails,
  )
}
