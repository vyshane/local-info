// Copyright 2019 Vy-Shane Xie

package mu.node.reversegeocoder

import java.util.UUID

import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import zone.overlap.localinfo.service.LocalInfoService
import zone.overlap.localinfo.v1.local_info.{GetLocalInfoRequest, LocalInfo, LocalInfoGrpcMonix}

// Exercise the gRPC server (in-process)
class GrpcServerIntegrationSpec extends AsyncWordSpec with Matchers with AsyncMockFactory with BeforeAndAfterAll {

  val serverName = s"local-info-test-server-${UUID.randomUUID().toString}"
  val localInfoService = mock[LocalInfoService]

  val server = InProcessServerBuilder
    .forName(serverName)
    .addService(
      LocalInfoGrpcMonix.bindService(localInfoService, monix.execution.Scheduler.global)
    )
    .directExecutor()
    .build()
    .start()

  val channel = InProcessChannelBuilder
    .forName(serverName)
    .directExecutor()
    .build()

  val localInfoClient = LocalInfoGrpcMonix.stub(channel)

  override def afterAll(): Unit = {
    channel.shutdownNow()
    server.shutdownNow()
  }

  "The local-info gRPC server" when {
    "a request is sent to get local info for a location" should {
      "send a response back" in {
        val response = LocalInfo()

        (localInfoService.getLocalInfo _)
          .expects(*)
          .returns(Task.now(response))

        localInfoClient
          .getLocalInfo(GetLocalInfoRequest())
          .runAsync
          .map { result =>
            result shouldEqual response
          }
      }
    }
  }
}
