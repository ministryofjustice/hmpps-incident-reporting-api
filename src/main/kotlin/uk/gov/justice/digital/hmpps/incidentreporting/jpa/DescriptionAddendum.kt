package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.DescriptionAddendum as DescriptionAddendumDto

@Entity
@EntityOpen
class DescriptionAddendum(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val report: Report,

  var createdBy: String,
  var firstName: String,
  var lastName: String,
  var createdAt: LocalDateTime,
  var text: String,
) : Comparable<DescriptionAddendum> {

  companion object {
    private val COMPARATOR = compareBy<DescriptionAddendum>
      { it.report }
      .thenBy { it.createdAt }
      .thenBy { it.createdBy }
      .thenBy { it.firstName }
      .thenBy { it.lastName }
      .thenBy { it.text }
  }

  override fun compareTo(other: DescriptionAddendum) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as DescriptionAddendum

    if (report != other.report) return false
    if (createdAt != other.createdAt) return false
    if (createdBy != other.createdBy) return false
    if (firstName != other.firstName) return false
    if (lastName != other.lastName) return false
    if (text != other.text) return false

    return true
  }

  override fun hashCode(): Int {
    var result = report.hashCode()
    result = 31 * result + createdAt.hashCode() + createdBy.hashCode()
    result = 31 * result + firstName.hashCode() + lastName.hashCode() + text.hashCode()
    return result
  }

  override fun toString(): String {
    return "DescriptionAddendum(id=$id, report=${report.reportReference}, " +
      "createdBy=$firstName $lastName, createdAt=$createdAt, text=$text)"
  }

  fun toDto() = DescriptionAddendumDto(
    createdBy = createdBy,
    firstName = firstName,
    lastName = lastName,
    createdAt = createdAt,
    text = text,
  )
}
