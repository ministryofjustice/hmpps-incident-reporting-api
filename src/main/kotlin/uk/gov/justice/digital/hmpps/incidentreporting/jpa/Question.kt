package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderColumn
import org.hibernate.Hibernate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Question as QuestionDto

@Entity
class Question(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  override val code: String,
  // TODO: should we force `question` to be non-null?
  override val question: String? = null,

  @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence")
  private val responses: MutableList<Response> = mutableListOf(),
) : DtoConvertible, GenericQuestion {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Question

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "Question(id=$id)"
  }

  fun getReport() = report

  override fun getResponses(): List<Response> = responses

  override fun addResponse(
    response: String,
    additionalInformation: String?,
    recordedBy: String,
    recordedOn: LocalDateTime,
  ): Question {
    responses.add(
      Response(
        question = this,
        response = response,
        recordedBy = recordedBy,
        recordedOn = recordedOn,
        additionalInformation = additionalInformation,
      ),
    )
    return this
  }

  override fun toDto() = QuestionDto(
    code = code,
    question = question,
    responses = responses.map { it.toDto() },
  )
}
