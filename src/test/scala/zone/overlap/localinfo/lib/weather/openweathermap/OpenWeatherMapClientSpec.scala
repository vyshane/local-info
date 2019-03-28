// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.openweathermap

import com.softwaremill.sttp._
import io.circe.Json
import io.circe.parser._
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers}
import zone.overlap.localinfo.lib.errors.Internal
import zone.overlap.localinfo.util.Fakes._
import zone.overlap.localinfo.v1.local_info.Language.{EN, FR}
import zone.overlap.localinfo.v1.local_info.MeasurementSystem.{IMPERIAL, METRIC}
import zone.overlap.localinfo.v1.local_info.Weather

class OpenWeatherMapClientSpec extends AsyncWordSpec with Matchers with AsyncMockFactory {

  val httpGetJson = mockFunction[Uri, Task[Json]]
  val apiKey = faker.internet().uuid()
  val client = new OpenWeatherMapClient(httpGetJson)(apiKey)

  val currentConditions = parse("""
      |{
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
      |    }
      |  ],
      |  "main": {
      |    "temp": 4.5,
      |    "pressure": 994,
      |    "humidity": 86,
      |    "temp_min": 3.33,
      |    "temp_max": 5.56
      |  },
      |  "wind": {
      |    "speed": 6.2,
      |    "deg": 340,
      |    "gust": 8.7
      |  },
      |  "clouds": {
      |    "all": 90
      |  }
      |}
    """.stripMargin).right.get

  val todaysForecast = parse("""
      |{
      |  "list": [
      |    {
      |      "temp": {
      |        "day": 285.51,
      |        "min": 281.19,
      |        "max": 288.22,
      |        "night": 285.51,
      |        "eve": 285.51,
      |        "morn": 285.51
      |      }
      |    }
      |  ]
      |}
    """.stripMargin).right.get

  "OpenWeatherMapClient" when {
    "asked to get the current weather" should {
      "fetch the current conditions as well as today's forecast temperatures" in {
        val coordinate = randomCoordinate()

        httpGetJson
          .expects(
            uri"https://api.openweathermap.org/data/2.5/weather?apikey=${apiKey}&lat=${coordinate.latitude}&lon=${coordinate.longitude}&lang=en&units=imperial"
          )
          .returning(Task.now(currentConditions))

        httpGetJson
          .expects(
            uri"https://api.openweathermap.org/data/2.5/forecast/daily?apikey=${apiKey}&cnt=1&lat=${coordinate.latitude}&lon=${coordinate.longitude}&lang=en&units=imperial"
          )
          .returning(Task.now(todaysForecast))

        client
          .getCurrentWeather(coordinate, EN, IMPERIAL)
          .runAsync
          .map(_ shouldBe a[Weather])
      }
    }
    "fetch of forecast temperatures fails" should {
      "return current weather but without forecast temperatures" in {
        val coordinate = randomCoordinate()

        httpGetJson
          .expects(
            uri"https://api.openweathermap.org/data/2.5/weather?apikey=${apiKey}&lat=${coordinate.latitude}&lon=${coordinate.longitude}&lang=fr&units=metric"
          )
          .returning(Task.now(currentConditions))

        httpGetJson
          .expects(
            uri"https://api.openweathermap.org/data/2.5/forecast/daily?apikey=${apiKey}&cnt=1&lat=${coordinate.latitude}&lon=${coordinate.longitude}&lang=fr&units=metric"
          )
          .returning(Task.raiseError(Internal("Test error").exception))

        client
          .getCurrentWeather(coordinate, FR, METRIC)
          .runAsync
          .map(_ shouldBe a[Weather])
      }
    }
    "fetch of current weather also fails" should {
      "result in an error" in {
        recoverToExceptionIf[StatusRuntimeException] {
          httpGetJson
            .expects(*)
            .twice()
            .returning(Task.raiseError(Internal("Test error").exception))
          client.getCurrentWeather(randomCoordinate(), randomLanguage(), randomMeasurementSystem()).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INTERNAL.getCode
        }
      }
    }
  }
}
