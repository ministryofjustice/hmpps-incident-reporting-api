package uk.gov.justice.digital.hmpps.incidentreporting.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import uk.gov.justice.digital.hmpps.incidentreporting.constants.TypeFamily

@DisplayName("Type codes")
class TypeCodeTest {
  @Test
  fun `codes for incident types start with family code`() {
    assertThat(Type.entries).allSatisfy { type ->
      val prefix = "${type.typeFamily.name}_"
      assertThat(type.name).startsWith(prefix)
      val suffix = type.name.removePrefix(prefix)
      suffix.toIntOrNull()
        ?: throw AssertionError("type code suffix should be an integer")
    }
  }

  @Test
  fun `codes for incident types within a family are numbered successively`() {
    assertThat(TypeFamily.entries).allSatisfy { family ->
      val prefix = "${family.name}_"
      val types = Type.entries.filter { it.typeFamily == family }
      val suffixes = types.map { it.name.removePrefix(prefix).toInt() }.toSet()
      assertThat(suffixes).hasSize(types.size)
      assertThat(suffixes.min()).isEqualTo(1)
      assertThat(suffixes.max()).isEqualTo(types.size)
    }
  }
}
