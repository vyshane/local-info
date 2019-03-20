// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather

import zone.overlap.localinfo.lib.utils.TaskUtils._
import io.circe.Json
import monix.eval.Task
import zone.overlap.localinfo.v1.local_info.Weather

case class ForecastTemperatures(minimum: Float, maximum: Float)

object OpenWeatherMapDecoder {

  def decodeCurrentWeather(json: Json): Task[Weather] = {
    val cursor = json.hcursor
    val weather = for {
      description <- cursor.downField("weather").downArray.first.get[String]("description")
      main = cursor.downField("main")
      temp <- main.get[Float]("temp")
      humidity <- main.get[Float]("humidity")
      pressure <- main.get[Float]("pressure")
      wind = cursor.downField("wind")
      windSpeed <- wind.get[Float]("speed")
      windDirection <- wind.get[Float]("deg")
      cloudCover <- cursor.downField("clouds").get[Float]("all")
    } yield Weather(description, temp, 0, 0, humidity, pressure, windSpeed, windDirection, cloudCover)

    toTask(weather)
  }

  def decodeTodaysForecastTemperatures(json: Json): Task[ForecastTemperatures] = {
    val temp = json.hcursor
      .downField("list")
      .downArray.first // We only need today's forecast
      .downField("temp")

    val forecastTemperatures = for {
      min <- temp.get[Float]("min")
      max <- temp.get[Float]("max")
    } yield ForecastTemperatures(min, max)

    toTask(forecastTemperatures)
  }
}
