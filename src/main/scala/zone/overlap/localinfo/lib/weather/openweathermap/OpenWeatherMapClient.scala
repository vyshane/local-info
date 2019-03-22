// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.openweathermap

import com.softwaremill.sttp._
import io.circe.Json
import monix.eval.Task
import zone.overlap.localinfo.lib.weather.WeatherClient
import zone.overlap.localinfo.lib.weather.openweathermap.OpenWeatherMapDecoder._
import zone.overlap.localinfo.v1.local_info.Language._
import zone.overlap.localinfo.v1.local_info.MeasurementSystem.{IMPERIAL, METRIC}
import zone.overlap.localinfo.v1.local_info.{Language, MeasurementSystem, Weather}
import zone.overlap.protobuf.coordinate.Coordinate

class OpenWeatherMapClient(httpGetJson: Uri => Task[Json])(apiKey: String) extends WeatherClient {

  val apiBaseUrl = "https://api.openweathermap.org/data/2.5"

  override def getCurrentWeather(coordinate: Coordinate,
                                 language: Language,
                                 measurementSystem: MeasurementSystem): Task[Weather] = {

    val currentConditions = fetchCurrentConditions(coordinate, language, measurementSystem)
    // OpenWeatherMap provides the day's minimum and maximum temparatures through the daily forecast API
    val forecastTemperatures = fetchTodaysForecastTemperatures(coordinate, language, measurementSystem)

    // Parallel requests
    Task.zipMap2(currentConditions, forecastTemperatures) { (weather, forecastTemperatures) =>
      weather
        .withMinimumTemperature(forecastTemperatures.minimum)
        .withMaximumTemperature(forecastTemperatures.maximum)
    }
  }

  private def fetchCurrentConditions(coordinate: Coordinate,
                                     language: Language,
                                     measurementSystem: MeasurementSystem): Task[Weather] = {
    val baseUri = uri"${apiBaseUrl}/weather"
      .param("apikey", apiKey)

    val uri = (
      withCoordinate(coordinate) _
        andThen withLang(language) _
        andThen withUnits(measurementSystem) _
        apply baseUri
    )

    for {
      json <- httpGetJson(uri)
      w <- decodeCurrentWeather(json)
    } yield w.withLanguage(language).withMeasurementSystem(measurementSystem)
  }

  private def fetchTodaysForecastTemperatures(coordinate: Coordinate,
                                              language: Language,
                                              measurementSystem: MeasurementSystem): Task[ForecastTemperatures] = {
    val baseUri = uri"${apiBaseUrl}/forecast/daily"
      .param("apikey", apiKey)
      .param("cnt", "1") // Number of days

    val uri = (
      withCoordinate(coordinate) _
        andThen withLang(language) _
        andThen withUnits(measurementSystem) _
        apply baseUri
    )

    for {
      json <- httpGetJson(uri)
      t <- decodeTodaysForecastTemperatures(json)
    } yield t
  }

  private def withCoordinate(coordinate: Coordinate)(uri: Uri): Uri = {
    uri
      .param("lat", String.valueOf(coordinate.latitude))
      .param("lon", String.valueOf(coordinate.longitude))
  }

  private def withUnits(measurementSystem: MeasurementSystem)(uri: Uri): Uri = {
    toSupportedUnitsParameter(measurementSystem) match {
      case None    => uri
      case Some(u) => uri.param("units", u)
    }
  }

  private def toSupportedUnitsParameter(measurementSystem: MeasurementSystem): Option[String] = measurementSystem match {
    case IMPERIAL => Some("imperial")
    case METRIC   => Some("metric")
    case _        => None
  }

  private def withLang(language: Language)(uri: Uri): Uri = {
    toSupportedLangParameter(language) match {
      case None    => uri
      case Some(l) => uri.param("lang", l)
    }
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
