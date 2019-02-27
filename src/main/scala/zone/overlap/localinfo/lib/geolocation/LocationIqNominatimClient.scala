// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import com.softwaremill.sttp._
import io.circe.Json
import monix.eval.Task
import zone.overlap.localinfo.lib.geolocation.NominatimAddressDecoder._
import zone.overlap.localinfo.v1.local_info.Language.LANGUAGE_UNSPECIFIED
import zone.overlap.localinfo.v1.local_info.{Language, Place}
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel
import zone.overlap.protobuf.zoom_level.ZoomLevel.ZOOM_LEVEL_UNSPECIFIED
import scala.util.Try

class LocationIqNominatimClient(httpGetJson: Uri => Task[Json])(apiToken: String) extends GeolocationClient {

  val apiBaseUrl = "http://us1.locationiq.com/v1/reverse.php?source=nom&key=${apiToken}&format=json&addressdetails=1"

  override def getPlace(coordinate: Coordinate, zoomLevel: ZoomLevel, language: Language): Task[Place] = {
    val languageParameter = toLanguageParameter(language).map(l => s"&accept-language=$l").getOrElse("")
    val zoomParameter = toZoomLevelParameter(zoomLevel).map(z => s"&zoom=$z").getOrElse("")

    val uri = Uri(
      uri"${apiBaseUrl}" +
        s"&lat=${coordinate.latitude}&lon=${coordinate.longitude}${languageParameter}${zoomParameter}"
    )

    for {
      json <- httpGetJson(uri)
      p <- decodePlace(json)
    } yield p
  }

  private def toZoomLevelParameter(zoomLevel: ZoomLevel): Option[Int] = zoomLevel match {
    case ZOOM_LEVEL_UNSPECIFIED =>
      None
    case _ =>
      Try(
        // Options are LEVEL_0, LEVEL_1, ... LEVEL_19
        Integer.parseInt(zoomLevel.name.split("_")(1))
      ).toOption
  }

  // See https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.10 for language parameter format
  private def toLanguageParameter(language: Language): Option[String] = language match {
    case LANGUAGE_UNSPECIFIED => None
    case _                    => Some(language.name.toLowerCase.replace("_", "-"))
  }
}
