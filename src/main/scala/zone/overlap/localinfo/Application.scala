// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

import com.typesafe.scalalogging.LazyLogging
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import mu.node.healthttpd.Healthttpd
import wvlet.airframe._
import zone.overlap.localinfo.service.LocalInfoService
import zone.overlap.localinfo.v1.local_info.LocalInfoGrpcMonix

trait Application extends LazyLogging {

  private val config = bind[Config]
  private val healthttpd = bind[Healthttpd]
  private val localInfoService = bind[LocalInfoService]

  def run(): Unit = {
    healthttpd.startAndIndicateNotReady()

    logger.info(s"Weather cache is ${if (config.weatherCacheEnabled) cacheEnabledInfo(config) else "disabled"}")
    logger.info("Starting gRPC server")

    val grpcServer = NettyServerBuilder
      .forPort(config.grpcPort)
      .addService(LocalInfoGrpcMonix.bindService(localInfoService, monix.execution.Scheduler.global))
      .build()
      .start()

    sys.ShutdownHookThread {
      grpcServer.shutdown()
      healthttpd.stop()
    }

    healthttpd.indicateReady()
    grpcServer.awaitTermination()
  }

  private def cacheEnabledInfo(config: Config): String = {
    s"enabled with a ${config.weatherCacheTtl} seconds ttl"
  }
}
