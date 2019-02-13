// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather
import zone.overlap.localinfo.v1.local_info.{Address, Language, MeasurementSystem}

package object cache {

  def generateLocalityKey(language: Language, measurementSystem: MeasurementSystem, address: Address): Option[String] = {
    if (language == Language.LANGUAGE_UNSPECIFIED) None
    else if (measurementSystem == MeasurementSystem.MEASUREMENT_SYSTEM_UNSPECIFIED) None
    else if (address.countryCode.isEmpty) None
    else if (address.city.isEmpty && address.cityDistrict.isEmpty && address.suburb.isEmpty) None
    else {
      val u: String => String = (value) => {
        if (value.isEmpty) "_"
        else value
      }
      Option(
        s"/${language.name}/${measurementSystem.name}/${address.countryCode}/${u(address.state)}/${u(address.county)}/" +
          s"${u(address.city)}/${u(address.cityDistrict)}/${u(address.suburb)}"
      )
    }
  }
}
