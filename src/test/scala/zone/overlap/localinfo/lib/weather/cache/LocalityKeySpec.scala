// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import org.scalatest.{Matchers, WordSpec}
import zone.overlap.localinfo.util.Fakes._
import zone.overlap.localinfo.v1.local_info.{Language, MeasurementSystem}

class LocalityKeySpec extends WordSpec with Matchers {

  "generateLocalityKey method" when {
    "called with unspecified language" should {
      "return empty option" in {
        generateLocalityKey(randomPlace(), Language.LANGUAGE_UNSPECIFIED, MeasurementSystem.METRIC) shouldEqual None
      }
    }
    "called with unspecified measurement system" should {
      "return empty option" in {
        generateLocalityKey(randomPlace(), Language.EN, MeasurementSystem.MEASUREMENT_SYSTEM_UNSPECIFIED) shouldEqual None
      }
    }
    "called with valid parameters" should {
      "return a properly formatted locality key" in {
        val language = randomLanguage()
        val measurementSystem = randomMeasurementSystem()
        val place = randomPlace()
        generateLocalityKey(place, language, measurementSystem) shouldEqual Some(
          s"/${place.name}/${language.name}/${measurementSystem.name}"
        )
      }
    }
  }
}
