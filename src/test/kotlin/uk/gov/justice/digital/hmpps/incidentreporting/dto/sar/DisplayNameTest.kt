package uk.gov.justice.digital.hmpps.incidentreporting.dto.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Names in a subject access request report")
class DisplayNameTest {
  @ParameterizedTest(name = "{0} reads as {1}")
  @CsvSource(
    "SMITH, Smith",
    "smith, Smith",
    "SCOTT-TRIMBLE, Scott-Trimble",
    "O'NEILL, O'Neill",
    "MARY ANNE, Mary Anne",
    "ÉLODIE, Élodie",
    "McDonald, McDonald",
    "van der Berg, van der Berg",
    "Smith, Smith",
  )
  fun `names recorded in a single case are capitalised and mixed-case names are left alone`(
    recorded: String,
    expected: String,
  ) {
    assertThat(displayName(recorded)).isEqualTo(expected)
  }
}
