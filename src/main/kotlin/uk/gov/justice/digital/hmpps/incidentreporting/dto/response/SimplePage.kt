package uk.gov.justice.digital.hmpps.incidentreporting.dto.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * A `org.springframework.data.domain.Page` that serialises better than the default implementation.
 * Removes excessive and redundant properties.
 */
@JsonIgnoreProperties(
  value = [
    "pageable",
    "first",
    "last",
    "empty",
  ],
)
class SimplePage<T>(
  content: List<T>,
  totalElements: Long,
  number: Int,
  size: Int,
  sort: Sort,
) : PageImpl<T>(content, PageRequest.of(number, size, sort), totalElements) {
  @get:Schema(description = "Sort orders", example = "[\"property,ASC\"]")
  @get:JsonProperty("sort")
  val sortOrderList: List<String>
    get() {
      return pageable.sort.stream().map { "${it.property},${it.direction}" }.toList()
    }
}

fun <T> Page<T>.toSimplePage(): SimplePage<T> = SimplePage(content, totalElements, pageable.pageNumber, pageable.pageSize, sort)
