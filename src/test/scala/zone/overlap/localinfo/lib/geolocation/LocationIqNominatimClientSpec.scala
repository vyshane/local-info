// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import com.softwaremill.sttp._
import io.circe.Json
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers}
import zone.overlap.localinfo.util.Fakes._
import zone.overlap.localinfo.v1.local_info.Place
import zone.overlap.localinfo.v1.local_info.Language.{EN, FR}
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel.LEVEL_10

class LocationIqNominatimClientSpec extends AsyncWordSpec with Matchers with AsyncMockFactory {

  val httpGetJson = mockFunction[Uri, Task[Json]]
  val apiToken = faker.internet().uuid()
  val client = new LocationIqNominatimClient(httpGetJson)(apiToken)

  "LocationIqNominatimClient" when {
    "getting place information in English" should {
      "only make one request to the Nominatim service" in {
        httpGetJson
          .expects(
            uri"http://us1.locationiq.com/v1/reverse.php?source=nom&key=${apiToken}&format=json&addressdetails=1&lat=1.0&lon=100.0&zoom=10&accept-language=en"
          )
          .once()
          .returning(Task.now(Json.Null))

        client
          .getPlace(Coordinate(1.0, 100.0), LEVEL_10, EN)
          .runAsync
          .map(_ shouldBe a[Place])
      }
    }
    "getting place information in a language other than English" should {
      "make two requests to the Nominatim service: One request in the provided language, and another in English" in {
        val language = FR

        httpGetJson
          .expects(
            uri"http://us1.locationiq.com/v1/reverse.php?source=nom&key=${apiToken}&format=json&addressdetails=1&lat=1.0&lon=100.0&zoom=10&accept-language=${language.name.toLowerCase}"
          )
          .returning(Task.now(Json.Null))

        // EN
        httpGetJson
          .expects(
            uri"http://us1.locationiq.com/v1/reverse.php?source=nom&key=${apiToken}&format=json&addressdetails=1&lat=1.0&lon=100.0&zoom=10&accept-language=en"
          )
          .returning(Task.now(Json.Null))

        client
          .getPlace(Coordinate(1.0, 100.0), LEVEL_10, language)
          .runAsync
          .map(_ shouldBe a[Place])
      }
    }
    "getting place information" should {
      "decode the fetched JSON into a Place" in (pending)
    }
  }
}
