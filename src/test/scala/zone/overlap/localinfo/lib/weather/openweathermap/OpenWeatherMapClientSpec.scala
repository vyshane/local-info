// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.openweathermap

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers}

class OpenWeatherMapClientSpec extends AsyncWordSpec with Matchers with AsyncMockFactory {

  "OpenWeatherMapClient" when {
    "asked to get the current weather" should {
      // TODO
      "fetch the current conditions as well as today's forecast temperatures" in (pending)
    }
    "fetch of forecast temperatures fails" should {
      // TODO
      "return current weather but without forecast temperatures" in (pending)
    }
  }
}
