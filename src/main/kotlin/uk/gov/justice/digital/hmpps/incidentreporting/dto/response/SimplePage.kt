package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort

/**
 * `org.springframework.data.domain.PageImpl` serialises with excessive and redundant properties.
 * This class creates simpler JSON.
 */
@Schema(description = "Page of results", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class SimplePage<T>(
  @param:Schema(description = "Elements in this pages", example = "[Item1,Item2]")
  val content: List<T>,
  @param:Schema(description = "Page number (0-based)", example = "0")
  val number: Int,
  @param:Schema(description = "Page size", example = "20")
  val size: Int,
  @param:Schema(description = "Total number of elements in all pages", example = "55")
  val totalElements: Long,
  @JsonIgnore
  val sort: Sort,
) {
  @Suppress("unused")
  @get:Schema(description = "Total number of pages", example = "3")
  @get:JsonProperty
  val totalPages: Int
    get() = kotlin.math.ceil(totalElements.toDouble() / size.toDouble()).toInt()

  @Suppress("unused")
  @get:Schema(description = "Number of elements in this page", example = "20")
  @get:JsonProperty
  val numberOfElements: Int
    get() = content.size

  @Suppress("unused")
  @get:Schema(description = "Sort orders", example = "[\"property,ASC\"]")
  @get:JsonProperty("sort")
  val sortOrderList: List<String>
    get() = sort.stream().map { "${it.property},${it.direction}" }.toList()
}

fun <T : Any> Page<T>.toSimplePage(): SimplePage<T> =
  SimplePage(content, pageable.pageNumber, pageable.pageSize, totalElements, sort)
