// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.utils

import io.circe.DecodingFailure
import monix.eval.Task
import zone.overlap.localinfo.lib.errors.Internal

object TaskUtils {

  def fromEither[A, B](t: A => Throwable)(e: Either[A, B]): Task[B] = {
    e match {
      case Left(a)  => Task.raiseError(t(a))
      case Right(b) => Task.now(b)
    }
  }

  def toTask[B](value: Either[DecodingFailure, B]): Task[B] = {
    fromEither[DecodingFailure, B](decodingFailure => Internal(decodingFailure.getMessage()).exception)(value)
  }
}
