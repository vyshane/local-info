// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

import com.apple.foundationdb.record.provider.foundationdb.{FDBDatabase, FDBDatabaseFactory}
import com.typesafe.scalalogging.LazyLogging
import mu.node.healthttpd.Healthttpd
import pureconfig.generic.auto._
import wvlet.airframe._

object Main extends App with LazyLogging {

  override def main(args: Array[String]): Unit = {
    val config = pureconfig.loadConfigOrThrow[Config]

    // Wire up dependencies
    newDesign
      .bind[Config].toInstance(config)
      .bind[Healthttpd].toInstance(Healthttpd(config.statusPort))
      .bind[FDBDatabase].toInstance(FDBDatabaseFactory.instance().getDatabase(config.foundationDbClusterFile))

      // Startup
      .withProductionMode
      .noLifeCycleLogging
      .withSession(_.build[Application].run())
  }
}
