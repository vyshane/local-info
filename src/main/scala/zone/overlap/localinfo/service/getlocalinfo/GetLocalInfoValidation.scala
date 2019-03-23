// Copyright 2019 Vy-Shane Xie Sin Fat

package zone.overlap.localinfo.service.getlocalinfo

import zone.overlap.localinfo.lib.validation.Validation

case object CoordinateIsRequired extends Validation {
  override def errorMessage: String = "Coordinate is required"
}

case object LongitudeOutOfRange extends Validation {
  override def errorMessage: String = "Longitude must be in range -180 and +180"
}

case object LatitudeOutOfRange extends Validation {
  override def errorMessage: String = "Latitude must be in range 0 and 90"
}

case object ZoomLevelIsRequired extends Validation {
  override def errorMessage: String = "Zoom level is required"
}
