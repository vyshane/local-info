// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.{Clock, Instant, ZoneId}
import com.google.protobuf.timestamp.Timestamp
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.PublishSubject
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers, OneInstancePerTest}
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import zone.overlap.localinfo.util.Fakes._
import scala.concurrent.duration._

class FoundationDbCacheSpec
    extends AsyncWordSpec
    with AsyncMockFactory
    with OneInstancePerTest
    with Matchers
    with LazyLogging {

  val cachedWeatherRepository = mock[CachedWeatherRepository]
  val purgeSignal = PublishSubject[Unit]()
  val now = Instant.now()
  val clock = Clock.fixed(now, ZoneId.systemDefault())
  val ttl = 30 seconds
  val foundationDbCache = new FoundationDbCache(cachedWeatherRepository, purgeSignal, clock, ttl)
  val localityKey = randomLocalityKey()

  "FoundationDbCache" when {
    "asked to get an item that doesn't exist" should {
      "return empty option" in {
        (cachedWeatherRepository.get _).expects(localityKey).returning(Task(None))
        foundationDbCache.get(localityKey).runAsync.map(_ shouldEqual None)
      }
    }
    "asked to get an item that has expired" should {
      "return empty option" in {
        val expiredCachedWeather = randomCachedWeather()
          .withCachedAt(Timestamp(now.minusSeconds(31).getEpochSecond))
        (cachedWeatherRepository.get _).expects(localityKey).returning(Task(Some(expiredCachedWeather)))
        foundationDbCache.get(localityKey).runAsync.map(_ shouldEqual None)
      }
    }
    "asked to get an item that has not expired" should {
      "return the item" in {
        val nonExipredCachedWeather = randomCachedWeather()
          .withCachedAt(Timestamp(now.minusSeconds(1).getEpochSecond))
        (cachedWeatherRepository.get _).expects(localityKey).returning(Task(Some(nonExipredCachedWeather)))
        foundationDbCache.get(localityKey).runAsync.map(_ shouldEqual Some(nonExipredCachedWeather))
      }
    }
    "asked to put an item" should {
      "save the cached weather" in {
        val weather = randomWeather()
        val cachedWeather = CachedWeather(localityKey, Some(weather), Some(Timestamp(now.getEpochSecond)))
        (cachedWeatherRepository.save _).expects(cachedWeather).returning(Task.unit)
        foundationDbCache.put(localityKey, weather).runAsync.map(_ shouldEqual ())
      }
    }
    "asked to purge expired items" should {
      "delete items older than the configured ttl" in {
        (cachedWeatherRepository.deleteOlderThan _).expects(now.minusSeconds(30)).returning(Task.unit)
        purgeSignal.onNext(())
        assert(true)
      }
    }
  }
}
