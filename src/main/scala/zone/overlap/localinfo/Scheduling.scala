// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

trait Scheduling {
  lazy val io: SchedulerService = Scheduler.io(name = "local-info-io")
}
