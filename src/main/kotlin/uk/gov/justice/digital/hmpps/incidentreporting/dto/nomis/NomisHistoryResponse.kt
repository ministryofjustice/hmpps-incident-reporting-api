package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisHistoryResponse(
  @param:Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @param:Schema(description = "The sequence number of the response for this incident")
  val responseSequence: Int,
  @param:Schema(description = "The answer text")
  val answer: String?,
  @param:Schema(description = "Response date added to the response by recording staff")
  val responseDate: LocalDate? = null,
  @param:Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @param:Schema(description = "Recording staff")
  val recordingStaff: NomisStaff,
)
