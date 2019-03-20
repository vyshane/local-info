// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import com.softwaremill.sttp._
import io.circe.Json
import monix.eval.Task
import zone.overlap.localinfo.lib.geolocation.NominatimAddressDecoder._
import zone.overlap.localinfo.v1.local_info.Language.{EN, LANGUAGE_UNSPECIFIED}
import zone.overlap.localinfo.v1.local_info.{Language, Place}
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel
import zone.overlap.protobuf.zoom_level.ZoomLevel.ZOOM_LEVEL_UNSPECIFIED
import scala.util.Try

class LocationIqNominatimClient(httpGetJson: Uri => Task[Json])(apiToken: String) extends GeolocationClient {

  override def getPlace(coordinate: Coordinate, zoomLevel: ZoomLevel, language: Language): Task[Place] = {
    val placeUri = getUri(coordinate, zoomLevel)(_)
    val fetch = fetchPlace(placeUri)(_)

    language match {
      case EN => fetch(EN)
      case _  =>
        // Also request for the address in English since place resource names
        // must always be generated from the English translation of the address
        Task.zipMap2(fetch(EN), fetch(language)) { (pEn, p) =>
          p.withName(pEn.name)
        }
    }
  }

  private def fetchPlace(placeUri: Language => Uri)(language: Language): Task[Place] = {
    httpGetJson(placeUri(language)) flatMap decodePlace
  }

  private def getUri(coordinate: Coordinate, zoomLevel: ZoomLevel)(language: Language): Uri = {
    val uri = uri"http://us1.locationiq.com/v1/reverse.php"
      .param("source", "nom")
      .param("key", apiToken)
      .param("format", "json")
      .param("addressdetails", "1")
      .param("lat", String.valueOf(coordinate.latitude))
      .param("lon", String.valueOf(coordinate.longitude))

    withZoomLevel(zoomLevel) _ andThen withLang(language) _ apply uri
  }

  private def withZoomLevel(zoomLevel: ZoomLevel)(uri: Uri): Uri = {
    toZoomLevelParameter(zoomLevel) match {
      case None    => uri
      case Some(z) => uri.param("zoom", String.valueOf(z))
    }
  }

  private def toZoomLevelParameter(zoomLevel: ZoomLevel): Option[Int] = zoomLevel match {
    case ZOOM_LEVEL_UNSPECIFIED =>
      None
    case _ =>
      Try {
        // Options are LEVEL_0, LEVEL_1, ... LEVEL_19
        Integer.parseInt(zoomLevel.name.split("_")(1))
      } toOption
  }

  private def withLang(language: Language)(uri: Uri): Uri = {
    toLanguageParameter(language) match {
      case None    => uri
      case Some(l) => uri.param("accept-language", l)
    }
  }

  // See https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.10 for language parameter format
  private def toLanguageParameter(language: Language): Option[String] = language match {
    case LANGUAGE_UNSPECIFIED => None
    case _                    => Some(language.name.toLowerCase.replace("_", "-"))
  }
}
