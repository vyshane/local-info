// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import java.time.Instant

import com.google.protobuf.timestamp.Timestamp
import org.scalatest.{AsyncWordSpec, BeforeAndAfterEach, Matchers}
import monix.execution.Scheduler.Implicits.global
import zone.overlap.localinfo.util.Fakes._

import scala.concurrent.Await
import scala.concurrent.duration._

/*
 * Integration tests for the CacheWeatherRepository
 * The tests in this spec need a Docker engine to run FoundationDB
 */
class CachedWeatherRepositorySpec
    extends AsyncWordSpec
    with Matchers
    with BeforeAndAfterEach
    with FoundationDbDockerTestKit {

  val repository = new CachedWeatherRepository(fdb, "local-info-tests")

  override def beforeEach(): Unit = {
    Await.result(repository.deleteOlderThan(Instant.now()).runAsync, 5 seconds)
  }

  "CachedWeatherRepository" when {
    "asked to get a record that doesn't exist" should {
      "return empty option" in {
        repository.get("inexistant").runAsync.map(_ shouldEqual None)
      }
    }
    "asked to save and retrieve a record" should {
      "return the record when retrieved again" in {
        val cachedWeather = randomCachedWeather()
        val retrieved = for {
          _ <- repository.save(cachedWeather)
          cw <- repository.get(cachedWeather.localityKey)
        } yield cw
        retrieved.runAsync.map(_ shouldEqual Option(cachedWeather))
      }
    }
    "asked to delete records older than an instant" should {
      "delete records older than the instant" in {
        val now = Instant.now()
        val cachedWeather = randomCachedWeather().withCachedAt(Timestamp(now.getEpochSecond))
        val result = for {
          _ <- repository.save(cachedWeather)
          found <- repository.get(cachedWeather.localityKey)
          _ <- repository.deleteOlderThan(now.plusSeconds(10))
          notFound <- repository.get(cachedWeather.localityKey)
        } yield (found, notFound)
        result.runAsync.map { r =>
          r._1 shouldEqual Option(cachedWeather)
          r._2 shouldBe None
        }
      }
      "not delete records created more recently than the instant" in {
        val now = Instant.now()
        val cachedWeather = randomCachedWeather().withCachedAt(Timestamp(now.getEpochSecond))
        val result = for {
          _ <- repository.save(cachedWeather)
          found <- repository.get(cachedWeather.localityKey)
          _ <- repository.deleteOlderThan(now.minusSeconds(10))
          stillFound <- repository.get(cachedWeather.localityKey)
        } yield (found, stillFound)
        result.runAsync.map { r =>
          r._1 shouldEqual Option(cachedWeather)
          r._2 shouldEqual Option(cachedWeather)
        }
      }
    }
  }
}
