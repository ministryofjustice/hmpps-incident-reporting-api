package uk.gov.justice.digital.hmpps.incidentreporting.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.incidentreporting.dto.utils.MaybeChanged

@DisplayName("Maybe-changed values")
class MaybeChangedTest {
  @Test
  fun `maybe-changed value calls block only if it was marked as changed`() {
    val unchangedValue = MaybeChanged.Unchanged("some value that is marked as unchanged")
    unchangedValue.alsoIfChanged {
      fail("block should not be called on unchanged values")
    }

    val changedValue = MaybeChanged.Changed("some value that is marked as changed")
    var blockCalled = false
    changedValue.alsoIfChanged {
      assertThat(it).isEqualTo("some value that is marked as changed")
      blockCalled = true
    }
    assertThat(blockCalled).isTrue()
  }
}
