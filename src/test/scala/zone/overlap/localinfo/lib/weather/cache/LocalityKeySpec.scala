// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import org.scalatest.{Matchers, WordSpec}
import zone.overlap.localinfo.util.Fakes._
import zone.overlap.localinfo.v1.local_info.{Language, MeasurementSystem}

class LocalityKeySpec extends WordSpec with Matchers {

  "generateLocalityKey method" when {
    "called with unspecified language" should {
      "return empty option" in {
        generateLocalityKey(Language.LANGUAGE_UNSPECIFIED, MeasurementSystem.METRIC, randomAddress()) shouldEqual None
      }
    }
    "called with unspecified measurement system" should {
      "return empty option" in {
        generateLocalityKey(Language.EN, MeasurementSystem.MEASUREMENT_SYSTEM_UNSPECIFIED, randomAddress()) shouldEqual None
      }
    }
    "called with an address without a country code" should {
      "return empty option" in {
        generateLocalityKey(Language.EN, MeasurementSystem.IMPERIAL, randomAddress().withCountryCode("")) shouldEqual None
      }
    }
    "called with an address without city, city district and suburb" should {
      "return empty option" in {
        generateLocalityKey(
          Language.EN,
          MeasurementSystem.IMPERIAL,
          randomAddress().withCity("").withCityDistrict("").withSuburb("")
        ) shouldEqual None
      }
    }
    "called with valid parameters" should {
      "return a properly formatted locality key" in {
        val language = randomLanguage()
        val measurementSystem = randomMeasurementSystem()
        val address = randomAddress()
        generateLocalityKey(language, measurementSystem, address) shouldEqual Some(
          s"/${language.name}/${measurementSystem.name}/${address.countryCode}/${address.state}/${address.county}/" +
            s"${address.city}/${address.cityDistrict}/${address.suburb}"
        )
      }
    }
    "called with a valid address with some blank fields" should {
      "return a locality key with _ in place of blank values" in {
        val language = randomLanguage()
        val measurementSystem = randomMeasurementSystem()
        val address = randomAddress().withState("").withCounty("").withCity("").withCityDistrict("")
        generateLocalityKey(language, measurementSystem, address) shouldEqual Some(
          s"/${language.name}/${measurementSystem.name}/${address.countryCode}/_/_/_/_/${address.suburb}"
        )
      }
    }
  }
}
