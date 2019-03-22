// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather

import io.circe.parser._
import io.grpc.{Status, StatusRuntimeException}
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{AsyncWordSpec, Matchers}
import zone.overlap.localinfo.lib.weather.OpenWeatherMapDecoder._
import zone.overlap.localinfo.v1.local_info.Language.EN
import zone.overlap.localinfo.v1.local_info.MeasurementSystem.METRIC
import zone.overlap.localinfo.v1.local_info.Weather

class OpenWeatherMapDecoderSpec extends AsyncWordSpec with Matchers {

  "OpenWeatherMapDecoder" when {
    "when asked to decode current weather from invalid JSON" should {
      "return an error" in {
        val invalidWeatherJson = parse("{\"invalid\": \"field\"}").right.get
        recoverToExceptionIf[StatusRuntimeException] {
          decodeCurrentWeather(invalidWeatherJson).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INTERNAL.getCode
        }
      }
    }
    "when asked to decode current weather from valid JSON" should {
      "decode the current weather" in {
        val currentWeatherNyc = parse(
          """{
            |  "coord": {
            |    "lon": -73.98,
            |    "lat": 40.76
            |  },
            |  "weather": [
            |    {
            |      "id": 500,
            |      "main": "Rain",
            |      "description": "light rain",
            |      "icon": "10d"
            |    },
            |    {
            |      "id": 300,
            |      "main": "Drizzle",
            |      "description": "light intensity drizzle",
            |      "icon": "09d"
            |    },
            |    {
            |      "id": 701,
            |      "main": "Mist",
            |      "description": "mist",
            |      "icon": "50d"
            |    }
            |  ],
            |  "base": "stations",
            |  "main": {
            |    "temp": 4.5,
            |    "pressure": 994,
            |    "humidity": 86,
            |    "temp_min": 3.33,
            |    "temp_max": 5.56
            |  },
            |  "visibility": 11265,
            |  "wind": {
            |    "speed": 6.2,
            |    "deg": 340,
            |    "gust": 8.7
            |  },
            |  "rain": {
            |    "1h": 0.41
            |  },
            |  "clouds": {
            |    "all": 90
            |  },
            |  "dt": 1553261434,
            |  "sys": {
            |    "type": 1,
            |    "id": 4686,
            |    "message": 0.0268,
            |    "country": "US",
            |    "sunrise": 1553252182,
            |    "sunset": 1553296156
            |  },
            |  "id": 5125771,
            |  "name": "Manhattan",
            |  "cod": 200
            |}
          """.stripMargin
        ).right.get

        decodeCurrentWeather(currentWeatherNyc).runAsync map {
          _ shouldEqual Weather(
            summary = "Light rain",
            temperature = 4.5f,
            minimumTemperature = 0,
            maximumTemperature = 0,
            humidity = 86,
            pressure = 994,
            windSpeed = 6.2f,
            windDirection = 340f,
            cloudCover = 90
          )
        }
      }
    }
    "when asked to decode today's forecast temperatures from invalid JSON" should {
      "return an error" in {
        val invalidForecastJson = parse("{\"invalid\": \"field\"}").right.get
        recoverToExceptionIf[StatusRuntimeException] {
          decodeTodaysForecastTemperatures(invalidForecastJson).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INTERNAL.getCode
        }
      }
    }
    "when asked to decode today's forecast temperatures from valid JSON" should {
      "decode today's forecast temperatures" in {
        val todaysForecastNyc = parse(
          """{
            |  "cod": "200",
            |  "message": 0,
            |  "city": {
            |    "geoname_id": 1907296,
            |    "name": "Tawarano",
            |    "lat": 35.0164,
            |    "lon": 139.0077,
            |    "country": "JP",
            |    "iso2": "JP",
            |    "type": "",
            |    "population": 0
            |  },
            |  "cnt": 1,
            |  "list": [
            |    {
            |      "dt": 1485741600,
            |      "temp": {
            |        "day": 285.51,
            |        "min": 281.19,
            |        "max": 288.22,
            |        "night": 285.51,
            |        "eve": 285.51,
            |        "morn": 285.51
            |      },
            |      "pressure": 1013.75,
            |      "humidity": 100,
            |      "weather": [
            |        {
            |          "id": 800,
            |          "main": "Clear",
            |          "description": "sky is clear",
            |          "icon": "01n"
            |        }
            |      ],
            |      "speed": 5.52,
            |      "deg": 311,
            |      "clouds": 0
            |    }
            |  ]
            |}
          """.stripMargin
        ).right.get

        decodeTodaysForecastTemperatures(todaysForecastNyc).runAsync map {
          _ shouldEqual ForecastTemperatures(281.19f, 288.22f)
        }
      }
    }
  }
}
