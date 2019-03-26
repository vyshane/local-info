// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service.getlocalinfo

import java.time.{Clock, LocalDate, ZoneId}
import java.util.Optional
import monix.eval.Task
import zone.overlap.localinfo.lib.geolocation.GeolocationClient
import zone.overlap.localinfo.lib.time.SunCalculator
import zone.overlap.localinfo.lib.validation._
import zone.overlap.localinfo.lib.weather.WeatherClient
import zone.overlap.localinfo.lib.weather.cache.{NoCache, WeatherCache, _}
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import zone.overlap.localinfo.service.getlocalinfo.GetLocalInfoValidator._
import zone.overlap.localinfo.v1.local_info._
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel
import scala.compat.java8.OptionConverters._

class GetLocalInfoRpc(geolocationClient: GeolocationClient,
                      weatherClient: WeatherClient,
                      weatherCache: WeatherCache = NoCache,
                      timeZoneQuery: (Double, Double) => Optional[ZoneId],
                      clock: Clock) {

  def handle(request: GetLocalInfoRequest): Task[LocalInfo] = {
    for {
      req <- toTask(validate(request))
      coordinate = req.coordinate.get
      zoomLevel = enforceZoomLevelRange(req.zoomLevel)
      place <- geolocationClient.getPlace(coordinate, zoomLevel, req.language)
      weather <- getWeather(coordinate, place, req.language, req.measurementSystem)
      sun = SunCalculator.calculateSun(LocalDate.now(clock), coordinate)
      timezone = timeZoneQuery(coordinate.latitude, coordinate.longitude).asScala.map(_.getId).getOrElse("")
    } yield LocalInfo(Some(coordinate), zoomLevel, Some(place), timezone, sun.rise, sun.set)
  }

  private def getWeather(coordinate: Coordinate,
                         place: Place,
                         language: Language,
                         measurementSystem: MeasurementSystem): Task[Weather] = {
    val localityKey = generateLocalityKey(place, language, measurementSystem)
    val cachedWeather: Task[Option[CachedWeather]] = localityKey.map(weatherCache.get).getOrElse(Task.now(None))

    cachedWeather.flatMap { cw =>
      cw match {
        case Some(cw) =>
          Task.now(cw.weather.get)
        case None =>
          weatherClient
            .getCurrentWeather(coordinate, language, measurementSystem)
            .flatMap(saveToCache(localityKey))
      }
    }
  }

  private def enforceZoomLevelRange(zoomLevel: ZoomLevel): ZoomLevel = {
    if (zoomLevel.value < ZoomLevel.LEVEL_5.value) ZoomLevel.LEVEL_5
    else if (zoomLevel.value > ZoomLevel.LEVEL_14.value) ZoomLevel.LEVEL_14
    else zoomLevel
  }

  private def saveToCache(localityKey: Option[String])(weather: Weather): Task[Weather] = {
    localityKey match {
      case Some(k) => weatherCache.put(k, weather).map(_ => weather)
      case None    => Task.now(weather)
    }
  }
}
