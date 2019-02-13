// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import org.scalatest.{AsyncWordSpec, Matchers}
import monix.execution.Scheduler.Implicits.global
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import zone.overlap.localinfo.util.Fakes._

/*
 * Integration tests for the CacheWeatherRepository
 * The tests in this spec need a Docker engine to run FoundationDB
 */
class CachedWeatherRepositorySpec extends AsyncWordSpec with Matchers with FoundationDbDockerTestKit {

  val repository = new CachedWeatherRepository(fdb, "local-info-tests")

  "CachedWeatherRepository" when {
    "asked to get a record that doesn't exist" should {
      "return empty option" in {
        repository.get("inexistant").runAsync.map(_ shouldEqual None)
      }
    }
    "asked to save and retrieve a record" should {
      "return the record when retrieved again" in {
        val language = randomLanguage()
        val measurementSystem = randomMeasurementSystem()
        val localityKey = generateLocalityKey(language, measurementSystem, randomAddress())
        val cachedWeather =
          CachedWeather(localityKey.get, Option(randomWeather(measurementSystem)), Option(randomTimestamp()))
        val retrieved = for {
          _ <- repository.save(cachedWeather)
          cw <- repository.get(localityKey.get)
        } yield cw
        retrieved.runAsync.map(_ shouldEqual Option(cachedWeather))
      }
    }
    "asked to save a record with a duplicate locality_key" should {
      "raise an error" in (pending)
    }
    "asked to delete a record doesn't exist" should {
      "return false" in (pending)
    }
    "asked to delete a record that is newer than the olderThan parameter" should {
      "not delete the record and return false" in (pending)
    }
    "asked to delete a record that is older than the olderThan parameter" should {
      "delete the record and return true" in (pending)
    }
  }
}
