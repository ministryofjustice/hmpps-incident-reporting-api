package uk.gov.justice.digital.hmpps.incidentreporting.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.incidentreporting.resource.ErrorCode

@DisplayName("Error codes")
class ErrorCodeTest {
  @Test
  fun `error codes should all be unique`() {
    val errorCodes = ErrorCode.entries
    val uniqueErrorCodes = errorCodes.map { it.errorCode }.toSet().size
    assertThat(errorCodes).hasSize(uniqueErrorCodes)
  }
}
