// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import io.circe.Json
import monix.eval.Task
import zone.overlap.localinfo.v1.local_info.Place

object NominatimAddressDecoder {

  def decodePlace(json: Json): Task[Place] = {
    val cursor = json.hcursor

    // TODO
    ???
  }

  def underscoreNonAlphanumericUnicode(value: String): String = {
    value.replaceAll("[^\\p{L}\\p{Nd}]+", "_")
  }
}
