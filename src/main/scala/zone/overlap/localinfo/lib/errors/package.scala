// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.errors
import io.grpc.{Status, StatusRuntimeException}

sealed trait ErrorStatus {}

case class Internal(message: String) extends ErrorStatus {
  def exception: StatusRuntimeException = {
    Status.INTERNAL.augmentDescription(message).asRuntimeException()
  }
}

case class InvalidArgument(message: String) extends ErrorStatus {
  def exception: StatusRuntimeException = {
    Status.INVALID_ARGUMENT.augmentDescription(message).asRuntimeException()
  }
}
