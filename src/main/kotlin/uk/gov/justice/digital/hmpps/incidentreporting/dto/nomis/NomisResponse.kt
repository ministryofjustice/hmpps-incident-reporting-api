package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisResponse(
  @param:Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @param:Schema(description = "The sequence number of the response for this incident")
  val sequence: Int,
  @param:Schema(description = "The answer text")
  val answer: String?,
  @param:Schema(description = "Response date added to the response by recording staff")
  val responseDate: LocalDate? = null,
  @param:Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @param:Schema(description = "Recording staff")
  val recordingStaff: NomisStaff,

  @param:Schema(description = "The date and time the response was created")
  val createDateTime: LocalDateTime,
  @param:Schema(description = "The username of the person who created the response")
  val createdBy: String,
  @param:Schema(description = "The date and time the response was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @param:Schema(description = "The username of the person who last updated the response")
  val lastModifiedBy: String? = createdBy,
)
