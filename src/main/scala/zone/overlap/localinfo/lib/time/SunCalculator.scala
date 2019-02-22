// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.time

import java.time.ZonedDateTime
import com.google.protobuf.timestamp.Timestamp
import net.time4j.{Moment, PlainDate}
import net.time4j.calendar.astro.{SolarTime, StdSolarCalculator}
import zone.overlap.protobuf.coordinate.Coordinate
import scala.compat.java8.OptionConverters._

case class Sun(rise: Option[Timestamp], set: Option[Timestamp])

object SunCalculator {

  def calculateSun(coordinate: Coordinate, altitudeMeters: Int, zonedDateTime: ZonedDateTime): Sun = {
    val solarTime =
      SolarTime.ofLocation(coordinate.latitude, coordinate.longitude, altitudeMeters, StdSolarCalculator.TIME4J)
    val calendarDate = PlainDate.from(zonedDateTime.toLocalDate)

    def toTimestamp(moment: Moment) = Timestamp(moment.getPosixTime, moment.getNanosecond())

    val rise = solarTime.sunrise().apply(calendarDate).asScala.map(toTimestamp)
    val set = solarTime.sunset().apply(calendarDate).asScala.map(toTimestamp)
    Sun(rise, set)
  }
}
