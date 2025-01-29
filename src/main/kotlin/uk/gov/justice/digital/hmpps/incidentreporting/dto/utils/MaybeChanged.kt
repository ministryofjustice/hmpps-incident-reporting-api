package uk.gov.justice.digital.hmpps.incidentreporting.dto.utils

/**
 * Represents a value that might have been changed,
 * allowing a block to be called only if it had been changed.
 */
sealed interface MaybeChanged<T> {
  val value: T

  fun alsoIfChanged(block: (T) -> Unit): MaybeChanged<T>

  data class Unchanged<T>(
    override val value: T,
  ) : MaybeChanged<T> {
    override fun alsoIfChanged(block: (T) -> Unit): Unchanged<T> {
      return this
    }
  }

  data class Changed<T>(
    override val value: T,
  ) : MaybeChanged<T> {
    override fun alsoIfChanged(block: (T) -> Unit): Changed<T> {
      block(value)
      return this
    }
  }
}
