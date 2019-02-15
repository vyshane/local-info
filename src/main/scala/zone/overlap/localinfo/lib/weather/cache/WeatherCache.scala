// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache
import java.time.{Clock, Instant}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import scala.concurrent.duration.Duration

sealed trait WeatherCache {}

case object NoCache extends WeatherCache

case class FoundationDbCache(cachedWeatherRepository: CachedWeatherRepository,
                             purgeSignal: Observable[Unit],
                             clock: Clock,
                             ttl: Duration)
    extends WeatherCache {

  purgeSignal.foreach(_ => purgeExpiredItems())

  def get(localityKey: String): Task[Option[CachedWeather]] = {
    cachedWeatherRepository
      .get(localityKey)
      .map {
        _.filter { cw =>
          val cutoff = Instant.now(clock).minusSeconds(ttl.toSeconds)
          wasRetrievedAfter(cw)(cutoff)
        }
      }
  }

  private def purgeExpiredItems(): Task[Unit] = {
    cachedWeatherRepository.deleteOlderThan(Instant.now(clock).minusSeconds(ttl.toSeconds))
  }

  private def wasRetrievedAfter(cachedWeather: CachedWeather)(instant: Instant): Boolean = {
    cachedWeather.retrievedAt
      .map(t => t.seconds > instant.getEpochSecond)
      .getOrElse(false)
  }
}
