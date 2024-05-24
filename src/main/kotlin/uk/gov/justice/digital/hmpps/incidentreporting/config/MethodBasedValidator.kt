package uk.gov.justice.digital.hmpps.incidentreporting.config

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.ValidationException
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext
import kotlin.reflect.KClass

interface ValidatableWithMethod {
  fun validate()
}

@Constraint(validatedBy = [MethodBasedValidator::class])
annotation class ValidateWithMethod(
  val message: String = "Invalid object ({error})",
  val groups: Array<KClass<out Any>> = [],
  val payload: Array<KClass<out Any>> = [],
)

class MethodBasedValidator : ConstraintValidator<ValidateWithMethod, ValidatableWithMethod> {
  override fun isValid(obj: ValidatableWithMethod, context: ConstraintValidatorContext?): Boolean {
    try {
      obj.validate()
      return true
    } catch (e: ValidationException) {
      context!!.unwrap(HibernateConstraintValidatorContext::class.java)
        .addMessageParameter("error", e.message)
      return false
    }
  }
}
