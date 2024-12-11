package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerOutcome
import uk.gov.justice.digital.hmpps.incidentreporting.constants.PrisonerRole
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdatePrisonerInvolvement
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.incidentreporting.dto.PrisonerInvolvement as PrisonerInvolvementDto

@Entity
@EntityOpen
class PrisonerInvolvement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val report: Report,

  val sequence: Int,

  var prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  var prisonerRole: PrisonerRole,

  @Enumerated(EnumType.STRING)
  var outcome: PrisonerOutcome? = null,

  var comment: String? = null,
) : Comparable<PrisonerInvolvement> {

  companion object {
    private val COMPARATOR = compareBy<PrisonerInvolvement>
      { it.report }
      .thenBy { it.sequence }
  }

  override fun compareTo(other: PrisonerInvolvement) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as PrisonerInvolvement

    if (report != other.report) return false
    if (sequence != other.sequence) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + sequence.hashCode()
    return result
  }

  override fun toString(): String {
    return "PrisonerInvolvement(id=$id, reportReference=${report.reportReference}, prisonerNumber=$prisonerNumber, prisonerRole=$prisonerRole, outcome=$outcome, comment=$comment)"
  }

  fun updateWith(request: UpdatePrisonerInvolvement) {
    request.prisonerNumber?.let { prisonerNumber = it }
    request.prisonerRole?.let { prisonerRole = it }
    request.outcome?.let { outcome = it.getOrNull() }
    request.comment?.let { comment = it.getOrNull() }
  }

  fun toDto() = PrisonerInvolvementDto(
    sequence = sequence,
    prisonerNumber = prisonerNumber,
    prisonerRole = prisonerRole,
    outcome = outcome,
    comment = comment,
  )
}
