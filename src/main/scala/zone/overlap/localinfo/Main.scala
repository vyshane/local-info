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
        .bind[CachedWeatherRepository].toProvider(cachedWeatherRepositoryProvider)
        .bind[FoundationDbCache].toSingletonProvider(foundationDbCacheProvider)
        .bind[GetLocalInfoRpc].toProvider(cachedGetLocalInfoRpcProvider)
    } else {
      design
        .bind[GetLocalInfoRpc].toInstance(new GetLocalInfoRpc(NoCache))
    }

    // Startup
    design.withProductionMode.noLifeCycleLogging
      .withSession(_.build[Application].run())

    lazy val cachedWeatherRepositoryProvider: FDBDatabase => CachedWeatherRepository = { db =>
      new CachedWeatherRepository(db, config.fdbKeySpaceDirectory)
    }

    lazy val foundationDbCacheProvider: CachedWeatherRepository => FoundationDbCache = { repository =>
      val purgeSignal = Observable.interval(1 minute).map(_ => ())
      FoundationDbCache(repository, purgeSignal, Clock.systemUTC(), 30 minutes)
    }

    lazy val cachedGetLocalInfoRpcProvider: FoundationDbCache => GetLocalInfoRpc = { foundationDbCache =>
      new GetLocalInfoRpc(foundationDbCache)
    }
  }
}
