// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather
import com.softwaremill.sttp._
import io.circe.Json
import io.circe.parser._
import monix.eval.Task
import zone.overlap.localinfo.lib.errors.Internal
import zone.overlap.localinfo.lib.utils.TaskUtils._
import zone.overlap.localinfo.lib.weather.OpenWeatherMapDecoder._
import zone.overlap.localinfo.v1.local_info.Language._
import zone.overlap.localinfo.v1.local_info.MeasurementSystem.{IMPERIAL, METRIC}
import zone.overlap.localinfo.v1.local_info.{Language, MeasurementSystem, Weather}
import zone.overlap.protobuf.coordinate.Coordinate

class OpenWeatherMapClient(apiKey: String) extends WeatherClient {

  implicit val backend = HttpURLConnectionBackend()
  val apiBaseUrl = "http://api.openweathermap.org/data/2.5"

  override def getCurrentWeather(coordinate: Coordinate,
                                 language: Language,
                                 measurementSystem: MeasurementSystem): Task[Weather] = {
    ???
  }

  private def fetchCurrentConditions(coordinate: Coordinate,
                                     language: Language,
                                     measurementSystem: MeasurementSystem): Task[Weather] = {
    val uri = s"${apiBaseUrl}/weather" +
      s"?apikey=$apiKey&lat=${coordinate.latitude}&lon=${coordinate.longitude}" +
      localeUriParameters(language, measurementSystem)

    ???
  }

  private def fetchForecastTemperatures(coordinate: Coordinate,
                                        language: Language,
                                        measurementSystem: MeasurementSystem): Task[ForecastTemperatures] = {
    val numberOfDays = 1 // Max 16
    val uri = s"${apiBaseUrl}/forecast/daily" +
      s"?apikey=$apiKey&lat=${coordinate.latitude}&lon=${coordinate.longitude}" +
      localeUriParameters(language, measurementSystem)

    for {
      body <- get(uri)
      json <- parseJson(body)
      t <- decodeForecastTemperatures(json)
    } yield t
  }

  private def get(uri: String): Task[Either[String, String]] = {
    Task(sttp.get(uri"$uri").send().body)
  }

  private def parseJson(body: Either[String, String]): Task[Json] = {
    fromEither[java.io.Serializable, Json](m => Internal(m.toString).exception)(body.flatMap(parse))
  }

  private def localeUriParameters(language: Language, measurementSystem: MeasurementSystem): String = {
    val unitsParam = toSupportedUnitsParameter(measurementSystem)
      .map(u => s"&units=$u")
      .getOrElse("")
    val langParam = toSupportedLangParameter(language)
      .map(l => s"&lang=$l")
      .getOrElse("")
    unitsParam + langParam
  }

  private def toSupportedUnitsParameter(measurementSystem: MeasurementSystem): Option[String] = measurementSystem match {
    case IMPERIAL => Some("imperial")
    case METRIC   => Some("metric")
    case _        => None
  }

  private def toSupportedLangParameter(language: Language): Option[String] = language match {
    case CS => Some("cz")
    case KO => Some("kr")
    case LV => Some("la")
    case SV => Some("se")
    case UK => Some("ua")
    case AR | BG | CA | DE | EL | EN | FA | FI | FR | GL | HR | HU | IT | JA | LT | MK | NL | PL | PT | RO | RU | SK | SL |
        ES | TK | VI | ZH_CN | ZH_TW =>
      Some(language.name.toLowerCase)
    case _ => None
  }
}
