// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import com.apple.foundationdb.record.provider.foundationdb.FDBDatabaseFactory
import com.spotify.docker.client.messages.PortBinding
import com.whisk.docker.testkit.{ContainerSpec, DockerReadyChecker, ManagedContainers}
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite

/*
 * Provides a FoundationDB Docker container for integration tests
 */
trait FoundationDbDockerTestKit extends DockerTestKitForAll {
  self: Suite =>

  private val fdbPort = 4500

  val fdb = FDBDatabaseFactory.instance().getDatabase(getClass.getResource("/fdb.cluster").getPath)

  lazy val fdbContainer = ContainerSpec("foundationdb/foundationdb:latest")
    .withPortBindings(fdbPort -> PortBinding.of("0.0.0.0", fdbPort))
    .withEnv("FDB_NETWORKING_MODE=host", s"FDB_PORT=$fdbPort")
    .withReadyChecker(DockerReadyChecker.LogLineContains("FDBD joined cluster."))

  override val managedContainers: ManagedContainers = fdbContainer.toManagedContainer
}
