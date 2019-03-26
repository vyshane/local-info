// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

import java.time.Clock
import java.util.concurrent.ThreadLocalRandom
import com.apple.foundationdb.record.provider.foundationdb.{FDBDatabase, FDBDatabaseFactory}
import com.typesafe.scalalogging.LazyLogging
import monix.reactive.Observable
import mu.node.healthttpd.Healthttpd
import net.iakovlev.timeshape.TimeZoneEngine
import pureconfig.generic.auto._
import wvlet.airframe._
import zone.overlap.localinfo.lib.geolocation.{GeolocationClient, LocationIqNominatimClient}
import zone.overlap.localinfo.lib.http.HttpClient._
import zone.overlap.localinfo.lib.weather.WeatherClient
import zone.overlap.localinfo.lib.weather.openweathermap.OpenWeatherMapClient
import zone.overlap.localinfo.lib.weather.cache.{CachedWeatherRepository, FoundationDbCache, NoCache}
import zone.overlap.localinfo.service.getlocalinfo.GetLocalInfoRpc
import scala.concurrent.duration._

object Main extends App with LazyLogging {

  override def main(args: Array[String]): Unit = {
    val config = pureconfig.loadConfigOrThrow[Config]

    // Wire up dependencies
    val design = newDesign
      .bind[Clock].toInstance(Clock.systemUTC())
      .bind[Config].toInstance(config)
      .bind[Healthttpd].toInstance(Healthttpd(config.statusPort))
      .bind[WeatherClient].toInstance(new OpenWeatherMapClient(httpGetJson)(config.openWeatherMapApiKey))
      .bind[GeolocationClient].toInstance(new LocationIqNominatimClient(httpGetJson)(config.locationIqToken))
      .bind[TimeZoneEngine].toInstance(TimeZoneEngine.initialize())

    if (config.weatherCacheEnabled) {
      design
        .bind[FDBDatabase].toInstance(FDBDatabaseFactory.instance().getDatabase(config.fdbClusterFile))
        .bind[CachedWeatherRepository].toSingletonProvider(cachedWeatherRepositoryProvider)
        .bind[FoundationDbCache].toSingletonProvider(foundationDbCacheProvider)
        .bind[GetLocalInfoRpc].toSingletonProvider(cachedGetLocalInfoRpcProvider)
    } else {
      design
        .bind[GetLocalInfoRpc].toSingletonProvider(unCachedGetLocalInfoRpcProvider)
    }

    // Start application
    design.withProductionMode.noLifeCycleLogging
      .withSession(_.build[Application].run())
  }

  private val cachedWeatherRepositoryProvider: (FDBDatabase, Config) => CachedWeatherRepository = { (db, config) =>
    new CachedWeatherRepository(db, config.fdbKeySpaceDirectory)
  }

  private val foundationDbCacheProvider: (CachedWeatherRepository, Config, Clock) => FoundationDbCache = {
    (repository, config, clock) =>
      // It is common to deploy multiple instances of the application at the same time.
      // Add some jitter so that the applications don't all purge the cache around the same time.
      val initialDelay = ThreadLocalRandom.current().nextInt(10, 51) seconds

      val purgeSignal = Observable.intervalWithFixedDelay(initialDelay, 1 minute).map(_ => ())
      new FoundationDbCache(repository, purgeSignal, clock, config.weatherCacheTtl seconds)
  }

  private val unCachedGetLocalInfoRpcProvider
    : (GeolocationClient, WeatherClient, TimeZoneEngine, Clock) => GetLocalInfoRpc = {
    (geolocationClient, weatherClient, timeZoneEngine, clock) =>
      new GetLocalInfoRpc(geolocationClient, weatherClient, NoCache, timeZoneEngine.query, clock)
  }

  private val cachedGetLocalInfoRpcProvider
    : (GeolocationClient, WeatherClient, FoundationDbCache, TimeZoneEngine, Clock) => GetLocalInfoRpc = {
    (geolocationClient, weatherClient, foundationDbCache, timeZoneEngine, clock) =>
      new GetLocalInfoRpc(geolocationClient, weatherClient, foundationDbCache, timeZoneEngine.query, clock)
  }
}
