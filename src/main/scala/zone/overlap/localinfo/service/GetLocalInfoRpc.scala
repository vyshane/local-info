// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service

import monix.eval.Task
import zone.overlap.localinfo.lib.weather.cache.{NoCache, WeatherCache}
import zone.overlap.localinfo.v1.local_info.{GetLocalInfoRequest, LocalInfo}

class GetLocalInfoRpc(weatherCache: WeatherCache = NoCache) {

  // TODO
  def handle(request: GetLocalInfoRequest): Task[LocalInfo] = ???
}
