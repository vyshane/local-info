// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

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
import zone.overlap.localinfo.v1.local_info.Place
import zone.overlap.localinfo.v1.local_info.Language.{EN, FR}
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel.LEVEL_10

class LocationIqNominatimClientSpec extends AsyncWordSpec with Matchers with AsyncMockFactory {

  val httpGetJson = mockFunction[Uri, Task[Json]]
  val apiToken = faker.internet().uuid()
  val client = new LocationIqNominatimClient(httpGetJson)(apiToken)

  val singapore = parse(
    """{
      |    "place_id": "208977371",
      |    "licence": "https://locationiq.com/attribution",
      |    "osm_type": "relation",
      |    "osm_id": "536780",
      |    "lat": "1.357107",
      |    "lon": "103.8194992",
      |    "display_name": "Singapore",
      |    "address": {
      |        "country": "Singapore",
      |        "country_code": "sg"
      |    },
      |    "boundingbox": [
      |        "1.1303611",
      |        "1.5131602",
      |        "103.5666667",
      |        "104.5706795"
      |    ]
      |}
    """.stripMargin).right.get

  "LocationIqNominatimClient" when {
    "getting place information in English" should {
      "only make one request to the Nominatim service" in {
        httpGetJson
          .expects(
            uri"http://us1.locationiq.com/v1/reverse.php?source=nom&key=${apiToken}&format=json&addressdetails=1&lat=1.0&lon=100.0&zoom=10&accept-language=en"
          )
          .once()
          .returning(Task.now(singapore))

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
          .returning(Task.now(singapore))

        // EN
        httpGetJson
          .expects(
            uri"http://us1.locationiq.com/v1/reverse.php?source=nom&key=${apiToken}&format=json&addressdetails=1&lat=1.0&lon=100.0&zoom=10&accept-language=en"
          )
          .returning(Task.now(singapore))

        client
          .getPlace(Coordinate(1.0, 100.0), LEVEL_10, language)
          .runAsync
          .map(_ shouldBe a[Place])
      }
    }
    "an error occurs while fetching place information" should {
      "return an error" in {
        recoverToExceptionIf[StatusRuntimeException] {
          httpGetJson
            .expects(*)
            .returning(Task.raiseError(Internal("Test error").exception))
          client.getPlace(Coordinate(1.0, 100.0), LEVEL_10, EN).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INTERNAL.getCode
        }
      }
    }
  }
}
