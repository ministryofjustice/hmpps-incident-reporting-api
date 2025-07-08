package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Addendum to the description of an incident", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class DescriptionAddendum(
  // TODO: sequences are only being exposed while we sort out sync problems: they do not need to remain in the api contract
  @param:Schema(description = "Sequence of the addendums for this report", deprecated = true)
  val sequence: Int,
  @param:Schema(description = "Username of user who added this addendum", example = "USER_1")
  val createdBy: String,
  @param:Schema(description = "When addendum was added", example = "2024-04-29T12:34:56.789012")
  val createdAt: LocalDateTime,
  @param:Schema(description = "First name of person that added this addendum", example = "John")
  val firstName: String,
  @param:Schema(description = "Last name of person that added this addendum", example = "Doe")
  val lastName: String,
  @param:Schema(description = "Addendum text")
  val text: String,
)
