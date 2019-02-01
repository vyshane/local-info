// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service

import monix.eval.Task
import zone.overlap.localinfo.v1.{GetLocalInfoRequest, LocalInfo}

class GetLocalInfoRpc {

  // TODO
  def handle(request: GetLocalInfoRequest): Task[LocalInfo] = ???
}
