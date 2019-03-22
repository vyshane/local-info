// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import io.circe.parser._
import io.grpc.{Status, StatusRuntimeException}
import monix.execution.Scheduler.Implicits.global
import org.scalatest.{AsyncWordSpec, Matchers}
import zone.overlap.localinfo.lib.geolocation.NominatimAddressDecoder._
import zone.overlap.localinfo.lib.utils.Base62._
import zone.overlap.localinfo.v1.local_info.Place
import zone.overlap.protobuf.country_code.CountryCode.{SG, US}

class NominatimAddressDecoderSpec extends AsyncWordSpec with Matchers {

  "NominatimAddressDecoder" when {
    "when asked to decode a place from invalid JSON" should {
      "return an error" in {
        val invalidJson = parse("{\"invalid\": \"field\"}").right.get
        recoverToExceptionIf[StatusRuntimeException] {
          decodePlace(invalidJson).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INTERNAL.getCode
        }
      }
    }
    "when asked to decode a place from valid JSON" should {
      "decode the place" in {
        val nyc = parse(
          """{
            |    "place_id": "209927283",
            |    "licence": "https://locationiq.com/attribution",
            |    "osm_type": "relation",
            |    "osm_id": "8398091",
            |    "lat": "40.7598219",
            |    "lon": "-73.9724708",
            |    "display_name": "Midtown East, Manhattan, Manhattan Community Board 5, New York County, New York City, New York, USA",
            |    "address": {
            |        "suburb": "Midtown East",
            |        "city_district": "Manhattan",
            |        "city": "New York City",
            |        "county": "New York County",
            |        "state": "New York",
            |        "country": "USA",
            |        "country_code": "us",
            |        "postcode": "10022"
            |    },
            |    "boundingbox": [
            |        "40.7498295",
            |        "40.7642791",
            |        "-73.9808875",
            |        "-73.9585367"
            |    ]
            |}
          """.stripMargin
        ).right.get

        val resourceName = "US." +
          encode("New York") + "." +
          encode("New York County") + "." +
          encode("New York City") + "." +
          encode("Manhattan") + "." +
          encode("Midtown East")

        decodePlace(nyc).runAsync map {
          _ shouldEqual Place(
            name = resourceName,
            countryCode = US,
            displayName = "Midtown East",
            address = "Midtown East, Manhattan, Manhattan Community Board 5, New York County, New York City, New York, USA",
            country = "USA"
          )
        }
      }
    }
    "when asked to decode an place with only country as the only address field" should {
      "successfully decode the place" in {
        val singapore = parse("""{
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

        decodePlace(singapore).runAsync map {
          _ shouldEqual Place(
            name = "SG." + encode("Singapore"),
            countryCode = SG,
            displayName = "Singapore",
            address = "Singapore",
            country = "Singapore"
          )
        }
      }
    }
  }
}
