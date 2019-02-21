// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.utils
import monix.eval.Task

object TaskUtils {

  def fromEither[A, B](t: A => Throwable)(e: Either[A, B]): Task[B] = {
    e match {
      case Left(a)  => Task.raiseError(t(a))
      case Right(b) => Task.now(b)
    }
  }
}
