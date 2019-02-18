// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

import java.time.Clock

import com.apple.foundationdb.record.provider.foundationdb.{FDBDatabase, FDBDatabaseFactory}
import com.typesafe.scalalogging.LazyLogging
import monix.reactive.Observable
import mu.node.healthttpd.Healthttpd
import pureconfig.generic.auto._
import wvlet.airframe._
import zone.overlap.localinfo.lib.weather.cache.{CachedWeatherRepository, FoundationDbCache, NoCache}
import zone.overlap.localinfo.service.GetLocalInfoRpc
import scala.concurrent.duration._

object Main extends App with LazyLogging {

  override def main(args: Array[String]): Unit = {
    val config = pureconfig.loadConfigOrThrow[Config]

    // Wire up dependencies
    val design = newDesign
      .bind[Config].toInstance(config)
      .bind[Healthttpd].toInstance(Healthttpd(config.statusPort))

    if (config.weatherCacheEnabled) {
      design
        .bind[FDBDatabase].toInstance(FDBDatabaseFactory.instance().getDatabase(config.fdbClusterFile))
        .bind[CachedWeatherRepository].toSingletonProvider(cachedWeatherRepositoryProvider)
        .bind[FoundationDbCache].toSingletonProvider(foundationDbCacheProvider)
        .bind[GetLocalInfoRpc].toSingletonProvider(cachedGetLocalInfoRpcProvider)
    } else {
      design
        .bind[GetLocalInfoRpc].toInstance(new GetLocalInfoRpc(NoCache))
    }

    // Start application
    design.withProductionMode.noLifeCycleLogging
      .withSession(_.build[Application].run())
  }

  private val cachedWeatherRepositoryProvider: (FDBDatabase, Config) => CachedWeatherRepository = { (db, config) =>
    new CachedWeatherRepository(db, config.fdbKeySpaceDirectory)
  }

  private val foundationDbCacheProvider: (CachedWeatherRepository, Config) => FoundationDbCache = { (repository, config) =>
    val purgeSignal = Observable.interval(1 minute).map(_ => ())
    FoundationDbCache(repository, purgeSignal, Clock.systemUTC(), config.weatherCacheTtl seconds)
  }

  private val cachedGetLocalInfoRpcProvider: FoundationDbCache => GetLocalInfoRpc = { foundationDbCache =>
    new GetLocalInfoRpc(foundationDbCache)
  }
}
