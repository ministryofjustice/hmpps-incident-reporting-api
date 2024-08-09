package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisResponse(
  @Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @Schema(description = "The sequence number of the response for this incident")
  val sequence: Int,
  @Schema(description = "The answer text")
  val answer: String?,
  @Schema(description = "Response date added to the response by recording staff")
  val responseDate: LocalDate? = null,
  @Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @Schema(description = "Recording staff")
  val recordingStaff: NomisStaff,

  @Schema(description = "The date and time the response was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the response")
  val createdBy: String,
  @Schema(description = "The date and time the response was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @Schema(description = "The username of the person who last updated the response")
  val lastModifiedBy: String? = createdBy,
)
