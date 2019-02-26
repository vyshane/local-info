// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.time

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import org.scalatest.{Matchers, WordSpec}
import zone.overlap.localinfo.lib.time.SunCalculator._
import zone.overlap.protobuf.coordinate.Coordinate

class SunCalculatorSpec extends WordSpec with Matchers {

  "SunCalculator" when {
    "asked to calculate sunrise and sunset times" should {
      "return the expected values" in {
        val quatreBornes = Coordinate(-20.265800, 57.479111)
        val sun = calculateSun(LocalDate.of(2019, 2, 26), quatreBornes, 0)
        val sunrise = LocalDateTime.ofInstant(Instant.ofEpochSecond(sun.rise.get.seconds), ZoneId.of("Indian/Mauritius"))
        val sunset = LocalDateTime.ofInstant(Instant.ofEpochSecond(sun.set.get.seconds), ZoneId.of("Indian/Mauritius"))
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        sunrise.format(formatter) shouldEqual "06:06"
        sunset.format(formatter) shouldEqual "18:39"
      }
    }
  }
}
