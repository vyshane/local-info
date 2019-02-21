// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather
import monix.eval.Task
import zone.overlap.localinfo.v1.local_info.{Language, MeasurementSystem, Weather}
import zone.overlap.protobuf.coordinate.Coordinate

trait WeatherClient {

  def getCurrentWeather(coordinate: Coordinate, language: Language, measurementSystem: MeasurementSystem): Task[Weather]
}
