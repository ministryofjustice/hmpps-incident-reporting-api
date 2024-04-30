package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn
import jakarta.validation.ValidationException
import org.hibernate.Hibernate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.HistoricalQuestion as HistoricalQuestionDto

@Entity
class HistoricalQuestion(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val history: History,

  override val code: String,
  // TODO: should we force `question` to be non-null?
  override val question: String? = null,

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence")
  @JoinColumn(name = "historical_question_id", nullable = false)
  private val responses: MutableList<HistoricalResponse> = mutableListOf(),

  @OneToOne(fetch = FetchType.LAZY)
  private var evidence: Evidence? = null,

  @OneToOne(fetch = FetchType.LAZY)
  private var location: Location? = null,

  @OneToOne(fetch = FetchType.LAZY)
  private var prisonerInvolvement: PrisonerInvolvement? = null,

  @OneToOne(fetch = FetchType.LAZY)
  private var staffInvolvement: StaffInvolvement? = null,
) : GenericQuestion {

  override fun addResponse(
    response: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): HistoricalQuestion {
    responses.add(
      HistoricalResponse(
        response = response,
        recordedBy = recordedBy,
        recordedOn = recordedOn,
        additionalInformation = additionalInformation,
      ),
    )
    return this
  }

  override fun getLocation() = location

  override fun attachLocation(location: Location): HistoricalQuestion {
    if (location.getReport() != history.getReport()) {
      throw ValidationException("Cannot attach a location from a different incident report")
    }
    this.location = location
    return this
  }

  override fun getPrisonerInvolvement() = prisonerInvolvement

  override fun attachPrisonerInvolvement(prisonerInvolvement: PrisonerInvolvement): HistoricalQuestion {
    if (prisonerInvolvement.getReport() != history.getReport()) {
      throw ValidationException("Cannot attach prisoner involvement from a different incident report")
    }
    this.prisonerInvolvement = prisonerInvolvement
    return this
  }

  override fun getStaffInvolvement() = staffInvolvement

  override fun attachStaffInvolvement(staffInvolvement: StaffInvolvement): HistoricalQuestion {
    if (staffInvolvement.getReport() != history.getReport()) {
      throw ValidationException("Cannot attach staff involvement from a different incident report")
    }
    this.staffInvolvement = staffInvolvement
    return this
  }

  override fun getEvidence() = evidence

  override fun attachEvidence(evidence: Evidence): HistoricalQuestion {
    if (evidence.getReport() != history.getReport()) {
      throw ValidationException("Cannot attach evidence from a different incident report")
    }
    this.evidence = evidence
    return this
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as HistoricalQuestion

    if (history != other.history) return false
    if (code != other.code) return false

    return true
  }

  override fun hashCode(): Int {
    var result = history.hashCode()
    result = 31 * result + code.hashCode()
    return result
  }

  override fun toString(): String {
    return "HistoricalQuestion(code='$code', responses=$responses)"
  }

  fun toDto() = HistoricalQuestionDto(
    code = code,
    question = question,
    responses = responses.map { it.toDto() },
  )
}
