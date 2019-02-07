// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service

import monix.eval.Task
import zone.overlap.localinfo.v1.local_info.{GetLocalInfoRequest, LocalInfo}
import zone.overlap.localinfo.v1.local_info.LocalInfoGrpcMonix

class LocalInfoService(getLocalInfoRpc: GetLocalInfoRpc) extends LocalInfoGrpcMonix.LocalInfoService {

  override def getLocalInfo(request: GetLocalInfoRequest): Task[LocalInfo] =
    getLocalInfoRpc.handle(request)
}
