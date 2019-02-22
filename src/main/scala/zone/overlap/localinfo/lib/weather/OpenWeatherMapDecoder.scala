// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather

import zone.overlap.localinfo.lib.utils.TaskUtils._
import io.circe.{DecodingFailure, Json}
import monix.eval.Task
import zone.overlap.localinfo.lib.errors.Internal
import zone.overlap.localinfo.v1.local_info.Weather

case class ForecastTemperatures(minimum: Float, maximum: Float)

object OpenWeatherMapDecoder {

  def decodeCurrentWeather(json: Json): Task[Weather] = {
    // TODO
    ???
  }

  def decodeTodaysForecastTemperatures(json: Json): Task[ForecastTemperatures] = {
    val temp = json.hcursor
      .downField("list")
      .downArray.first // We only need today's forecast
      .downField("temp")

    for {
      min <- decodeField(temp.get[Float]("min"))
      max <- decodeField(temp.get[Float]("max"))
      t <- Task.now(ForecastTemperatures(min, max))
    } yield t
  }

  def decodeField[B](value: Either[DecodingFailure, B]): Task[B] = {
    fromEither[DecodingFailure, B](decodingFailure => Internal(decodingFailure.getMessage()).exception)(value)
  }
}
