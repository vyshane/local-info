// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.{Clock, Instant}
import com.google.protobuf.timestamp.Timestamp
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import zone.overlap.localinfo.Scheduling
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import zone.overlap.localinfo.v1.local_info.Weather
import scala.concurrent.duration.Duration

sealed trait WeatherCache {
  def get(localityKey: String): Task[Option[CachedWeather]]
  def put(localityKey: String, weather: Weather): Task[Unit]
}

object NoCache extends WeatherCache {
  override def get(localityKey: String): Task[Option[CachedWeather]] = Task.now(None)
  override def put(localityKey: String, weather: Weather): Task[Unit] = Task.now(())
}

class FoundationDbCache(cachedWeatherRepository: CachedWeatherRepository,
                        purgeSignal: Observable[Unit],
                        clock: Clock,
                        ttl: Duration)
    extends WeatherCache
    with Scheduling
    with LazyLogging {

  purgeSignal.executeOn(io).foreach(_ => purgeExpiredItems())

  override def get(localityKey: String): Task[Option[CachedWeather]] = {
    cachedWeatherRepository
      .get(localityKey)
      .map {
        _.filter { cw =>
          val cutoff = Instant.now(clock).minusSeconds(ttl.toSeconds)
          wasRetrievedAfter(cw)(cutoff)
        }
      }
  }

  override def put(localityKey: String, weather: Weather): Task[Unit] = {
    cachedWeatherRepository.save(
      CachedWeather(
        localityKey,
        Some(weather),
        Some(Timestamp(Instant.now(clock).getEpochSecond))
      )
    )
  }

  private def purgeExpiredItems(): Task[Unit] = {
    logger.debug("Purging expired items from weather cache")
    cachedWeatherRepository.deleteOlderThan(Instant.now(clock).minusSeconds(ttl.toSeconds))
  }

  private def wasRetrievedAfter(cachedWeather: CachedWeather)(instant: Instant): Boolean = {
    cachedWeather.cachedAt
      .map(t => t.seconds > instant.getEpochSecond)
      .getOrElse(false)
  }
}
