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
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.constants.NO_DETAILS_GIVEN
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis.NomisReport
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.id.GeneratedUuidV7
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Event as EventDto
import uk.gov.justice.digital.hmpps.incidentreporting.dto.EventWithBasicReports as EventWithReportsDto

@Entity
@EntityOpen
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
  var id: UUID? = null,

  /**
   * Human-readable reference.
   * Matches incident number when sourced from NOMIS.
   */
  @Column(nullable = false, unique = true, length = 25)
  val eventReference: String,

  var eventDateAndTime: LocalDateTime,
  var location: String,

  var title: String,
  var description: String,

  @OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("id ASC")
  val reports: MutableList<Report> = mutableListOf(),

  var createdAt: LocalDateTime,
  var modifiedAt: LocalDateTime,
  var modifiedBy: String,
) : Comparable<Event> {

  companion object {
    fun createReport(nomisReport: NomisReport): Report {
      val event = Event(
        eventReference = "${nomisReport.incidentId}",
        eventDateAndTime = nomisReport.incidentDateTime,
        location = nomisReport.prison.code,
        title = nomisReport.title ?: NO_DETAILS_GIVEN,
        description = nomisReport.description ?: NO_DETAILS_GIVEN,
        createdAt = nomisReport.createDateTime,
        modifiedAt = nomisReport.lastModifiedDateTime ?: nomisReport.createDateTime,
        modifiedBy = nomisReport.lastModifiedBy ?: nomisReport.createdBy,
      )
      val status = Status.fromNomisCode(nomisReport.status.code)
      val (upsertDescription, upsertAddendums) = nomisReport.getDescriptionParts()
      val report = Report(
        reportReference = "${nomisReport.incidentId}",
        type = Type.fromNomisCode(nomisReport.type),
        incidentDateAndTime = nomisReport.incidentDateTime,
        location = nomisReport.prison.code,
        title = nomisReport.title ?: NO_DETAILS_GIVEN,
        description = upsertDescription ?: NO_DETAILS_GIVEN,
        reportedBy = nomisReport.reportingStaff.username,
        reportedAt = nomisReport.reportedDateTime,
        status = status,
        questionSetId = "${nomisReport.questionnaireId}",
        createdAt = nomisReport.createDateTime,
        modifiedAt = nomisReport.lastModifiedDateTime ?: nomisReport.createDateTime,
        modifiedBy = nomisReport.lastModifiedBy ?: nomisReport.createdBy,
        source = InformationSource.NOMIS,
        modifiedIn = InformationSource.NOMIS,
        assignedTo = nomisReport.reportingStaff.username,
        event = event,
      )
      report.addStatusHistory(status, nomisReport.reportedDateTime, nomisReport.reportingStaff.username)

      report.updateDescriptionAddendums(upsertAddendums)
      report.updateStaffInvolved(nomisReport.staffParties)
      report.updatePrisonerInvolved(nomisReport.offenderParties)
      report.updateCorrectionRequests(nomisReport.requirements)
      report.updateQuestionAndResponses(nomisReport.questions)
      report.updateHistory(nomisReport.history)

      return report
    }

    private val COMPARATOR = compareBy<Event>
      { it.eventReference }
  }

  override fun compareTo(other: Event) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Event

    return eventReference == other.eventReference
  }

  override fun hashCode(): Int {
    return eventReference.hashCode()
  }

  override fun toString(): String {
    return "Event(id=$id, eventReference=$eventReference)"
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
    location = location,
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
    location = location,
    eventDateAndTime = eventDateAndTime,
    title = title,
    description = description,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    modifiedBy = modifiedBy,
    reports = reports.map { it.toDtoBasic() },
  )
}
