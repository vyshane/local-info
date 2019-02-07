// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

import com.apple.foundationdb.record.provider.foundationdb.keyspace.{KeySpace, KeySpaceDirectory}
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
      .bind[FDBDatabase].toInstance(FDBDatabaseFactory.instance().getDatabase(config.fdbClusterFile))
      .bind[KeySpace].toInstance(new KeySpace(
        new KeySpaceDirectory(config.fdbKeySpaceDirectory, KeySpaceDirectory.KeyType.STRING)))

      // Startup
      .withProductionMode
      .noLifeCycleLogging
      .withSession(_.build[Application].run())
  }
}
