// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather

import zone.overlap.localinfo.v1.local_info.{Language, MeasurementSystem, Place}

package object cache {

  def generateLocalityKey(place: Place, language: Language, measurementSystem: MeasurementSystem): Option[String] = {
    if (language == Language.LANGUAGE_UNSPECIFIED || measurementSystem == MeasurementSystem.MEASUREMENT_SYSTEM_UNSPECIFIED)
      None
    else
      Some(s"/${place.name}/${language.name}/${measurementSystem.name}")
  }
}
