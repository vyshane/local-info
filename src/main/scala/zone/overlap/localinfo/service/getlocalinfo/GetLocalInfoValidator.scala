// Copyright 2019 Vy-Shane Xie Sin Fat

package zone.overlap.localinfo.service.getlocalinfo

import cats.implicits._
import zone.overlap.localinfo.lib.validation.ValidationResult
import zone.overlap.localinfo.v1.local_info.GetLocalInfoRequest
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel
import zone.overlap.protobuf.zoom_level.ZoomLevel.ZOOM_LEVEL_UNSPECIFIED

object GetLocalInfoValidator {

  def validate(request: GetLocalInfoRequest): ValidationResult[GetLocalInfoRequest] = {
    (validateCoordinate(request.coordinate), validateZoomLevel(request.zoomLevel)) mapN { (c, z) =>
      request.withCoordinate(c).withZoomLevel(z)
    }
  }

  private def validateCoordinate(coordinate: Option[Coordinate]): ValidationResult[Coordinate] = {
    coordinate match {
      case None => CoordinateIsRequired.invalidNec
      case Some(c) =>
        if (c.longitude < -180 || c.longitude > 180) LongitudeOutOfRange.invalidNec
        else if (c.latitude < 0 || c.latitude > 90) LatitudeOutOfRange.invalidNec
        else c.validNec
    }
  }

  private def validateZoomLevel(zoomLevel: ZoomLevel): ValidationResult[ZoomLevel] = {
    if (zoomLevel != ZOOM_LEVEL_UNSPECIFIED) ZoomLevelIsRequired.invalidNec
    else zoomLevel.validNec
  }
}
