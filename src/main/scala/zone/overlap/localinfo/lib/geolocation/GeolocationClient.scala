// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation
import monix.eval.Task
import zone.overlap.localinfo.v1.local_info.{Address, Language}
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel

trait GeolocationClient {

  def getAddress(coordinate: Coordinate, zoomLevel: ZoomLevel, language: Language): Task[Address]
}
