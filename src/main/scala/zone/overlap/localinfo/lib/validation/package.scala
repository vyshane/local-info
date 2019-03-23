// Copyright 2019 Vy-Shane Xie Sin Fat

package zone.overlap.localinfo.lib

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import monix.eval.Task
import zone.overlap.localinfo.lib.errors.InvalidArgument

package object validation {

  type ValidationResult[A] = ValidatedNec[Validation, A]

  def toTask[A](value: ValidationResult[A]): Task[A] = {
    value match {
      case Valid(value) => Task(value)
      case Invalid(nec) => {
        val errorDescription = nec.foldLeft("") { (acc, v) =>
          acc + "; " + v.errorMessage
        }
        Task.raiseError(InvalidArgument(errorDescription).exception)
      }
    }
  }
}
