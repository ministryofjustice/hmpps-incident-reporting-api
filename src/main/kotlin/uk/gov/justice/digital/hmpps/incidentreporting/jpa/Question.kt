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
import org.hibernate.annotations.BatchSize
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Question as QuestionDto

@Entity
class Question(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  // TODO: decide how this works and if it is ever unique (eg within 1 report)
  val code: String,
  val question: String,
  val additionalInformation: String? = null,

  @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderColumn(name = "sequence", nullable = false)
  @BatchSize(size = 10)
  private val responses: MutableList<Response> = mutableListOf(),
) {
  override fun toString(): String {
    return "Question(id=$id)"
  }

  fun getReport() = report

  fun getResponses(): List<Response> = responses

  fun addResponse(
    response: String,
    responseDate: LocalDate? = null,
    additionalInformation: String? = null,
    recordedBy: String,
    recordedAt: LocalDateTime,
  ): Question {
    responses.add(
      Response(
        question = this,
        response = response,
        responseDate = responseDate,
        additionalInformation = additionalInformation,
        recordedBy = recordedBy,
        recordedAt = recordedAt,
      ),
    )
    return this
  }

  fun toDto() = QuestionDto(
    code = code,
    question = question,
    additionalInformation = additionalInformation,
    responses = responses.map { it.toDto() },
  )
}
