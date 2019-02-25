// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service

import java.time.{Clock, ZonedDateTime}
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

    for {
      address <- geolocationClient.getAddress(request.coordinate.get, request.zoomLevel, request.language)
      weather <- getWeather(request.coordinate.get, address, request.language, request.measurementSystem)
      sun = SunCalculator.calculateSun(request.coordinate.get, 0, ZonedDateTime.now(clock))
      timezone = timeZoneEngine.query(request.coordinate.get.latitude, request.coordinate.get.longitude).asScala
    } yield ()

    ???
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
