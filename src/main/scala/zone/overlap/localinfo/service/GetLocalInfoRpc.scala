// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service

import java.time.{Clock, LocalDate}
import monix.eval.Task
import net.iakovlev.timeshape.TimeZoneEngine
import zone.overlap.localinfo.lib.geolocation.GeolocationClient
import zone.overlap.localinfo.lib.time.SunCalculator
import zone.overlap.localinfo.lib.weather.cache._
import zone.overlap.localinfo.lib.weather.WeatherClient
import zone.overlap.localinfo.lib.weather.cache.{NoCache, WeatherCache}
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import zone.overlap.localinfo.v1.local_info._
import zone.overlap.protobuf.coordinate.Coordinate
import scala.compat.java8.OptionConverters._

class GetLocalInfoRpc(geolocationClient: GeolocationClient,
                      weatherClient: WeatherClient,
                      weatherCache: WeatherCache = NoCache,
                      timeZoneEngine: TimeZoneEngine,
                      clock: Clock) {

  def handle(request: GetLocalInfoRequest): Task[LocalInfo] = {
    // TODO: Validate request
    val coordinate = request.coordinate.get

    for {
      address <- geolocationClient.getAddress(coordinate, request.zoomLevel, request.language)
      weather <- getWeather(coordinate, address, request.language, request.measurementSystem)
      sun = SunCalculator.calculateSun(LocalDate.now(clock), coordinate)
      timezone = timeZoneEngine.query(coordinate.latitude, coordinate.longitude).asScala.map(_.getId).getOrElse("")
    } yield LocalInfo(Some(coordinate), Some(address), timezone, sun.rise, sun.set)
  }

  private def getWeather(coordinate: Coordinate,
                         address: Address,
                         language: Language,
                         measurementSystem: MeasurementSystem): Task[Weather] = {
    val cachedWeather: Task[Option[CachedWeather]] = generateLocalityKey(language, measurementSystem, address)
      .map(weatherCache.get)
      .getOrElse(Task.now(None))

    cachedWeather.flatMap { cw =>
      cw match {
        case Some(c) => Task.now(c.weather.get)
        case None    => weatherClient.getCurrentWeather(coordinate, language, measurementSystem)
      }
    }
  }
}
