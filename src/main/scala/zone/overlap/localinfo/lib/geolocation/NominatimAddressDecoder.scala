// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation
import io.circe.Json
import monix.eval.Task
import zone.overlap.localinfo.v1.local_info.Address

object NominatimAddressDecoder {

  def decodeAddress(json: Json): Task[Address] = {
    // TODO
    ???
  }
}
