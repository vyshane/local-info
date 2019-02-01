// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service

import monix.eval.Task
import zone.overlap.localinfo.v1.{GetLocalInfoRequest, LocalInfo, LocalInfoGrpcMonix}

class LocalInfoService(getLocalInfoRpc: GetLocalInfoRpc) extends LocalInfoGrpcMonix.LocalInfoService {

  override def getLocalInfo(request: GetLocalInfoRequest): Task[LocalInfo] =
    getLocalInfoRpc.handle(request)
}
