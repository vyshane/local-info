// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.weather.cache

import com.apple.foundationdb.record.provider.foundationdb.FDBDatabaseFactory
import com.spotify.docker.client.messages.PortBinding
import com.whisk.docker.testkit.ContainerState.{HasId, Ready}
import com.whisk.docker.testkit.{
  BaseContainer,
  ContainerCommandExecutor,
  ContainerSpec,
  DockerReadyChecker,
  FailFastCheckException,
  ManagedContainers
}
import com.whisk.docker.testkit.scalatest.DockerTestKitForAll
import org.scalatest.Suite
import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides a FoundationDB Docker container for integration tests
 */
trait FoundationDbDockerTestKit extends DockerTestKitForAll {
  self: Suite =>

  private val fdbPort = 4500

  val fdb = FDBDatabaseFactory.instance().getDatabase(getClass.getResource("/fdb.cluster").getPath)

  lazy val fdbContainer = ContainerSpec("foundationdb/foundationdb:latest")
    .withPortBindings(fdbPort -> PortBinding.of("0.0.0.0", fdbPort))
    .withEnv("FDB_NETWORKING_MODE=host", s"FDB_PORT=$fdbPort")
    // FoundationDB Docker container doesn't come with a pre-configured database
    .withReadyChecker(new FdbDockerReadyChecker("configure new single memory"))

  override val managedContainers: ManagedContainers = fdbContainer.toManagedContainer
}

/**
 * Ready checker for FoundationDB container, with ability to run a fdbcli exec command
 * once fdb has started.
 */
class FdbDockerReadyChecker(onReadyFdbcliExec: String) extends DockerReadyChecker {

  override def apply(container: BaseContainer)(implicit docker: ContainerCommandExecutor,
                                               ec: ExecutionContext): Future[Unit] = {
    val execOnReady: (String) => Future[Unit] = (containerId) => {
      Future {
        docker.client.execCreate(
          containerId,
          Array("/usr/bin/fdbcli", "--exec", onReadyFdbcliExec)
        )
      } map { exec =>
        docker.client.execStart(exec.id()).readFully()
      } map (_ => ())
    }

    container.state() match {
      case Ready(info) =>
        execOnReady(info.id())
      case state: HasId =>
        docker
          .withLogStreamLinesRequirement(state.id, withErr = true)(_.contains("FDBD joined cluster."))
          .flatMap(_ => execOnReady(state.id))
      case _ =>
        Future.failed(new FailFastCheckException("Can't initialize LogStream to container without ID"))
    }
  }
}
