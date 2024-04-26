package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Response as ResponseDto

@Entity
class Response(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  override val response: String,

  override val recordedBy: String,

  override val recordedOn: LocalDateTime,

  override val additionalInformation: String? = null,
) : GenericResponse {
  fun toDto() = ResponseDto(
    response = response,
    recordedBy = recordedBy,
    recordedOn = recordedOn,
    additionalInformation = additionalInformation,
  )
}
