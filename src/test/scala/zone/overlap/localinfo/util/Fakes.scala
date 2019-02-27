// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.util

import java.util.concurrent.TimeUnit
import com.github.javafaker.Faker
import com.google.protobuf.timestamp.Timestamp
import zone.overlap.localinfo.persistence.cached_weather.CachedWeather
import zone.overlap.localinfo.v1.local_info.MeasurementSystem.{IMPERIAL, METRIC}
import zone.overlap.localinfo.v1.local_info._
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.localinfo.lib.weather.cache._

/*
 * Fake entity generators
 */
object Fakes {

  val faker = new Faker()

  def randomTimestamp(): Timestamp = {
    Timestamp(faker.date().past(1, TimeUnit.HOURS).toInstant.getEpochSecond, faker.random().nextInt(Int.MaxValue))
  }

  def randomCoordinate(): Coordinate = {
    Coordinate(faker.address().latitude().toDouble, faker.address().longitude().toDouble)
  }

  def randomLocalityKey(): String = {
    generateLocalityKey(randomPlace(), randomLanguage(), randomMeasurementSystem()).get
  }

  def randomPlace(): Place = {
    val address = faker.address()
    val resourceName =
      s"places/${address.countryCode()}.${address.state()}.${address.city()}"
        .toLowerCase().replaceAll("[^\\p{L}\\p{Nd}]+", "_")
    Place(
      resourceName,
      address.city(),
      s"${address.city()}, ${address.state()}, ${address.country()}",
      address.country(),
      address.countryCode()
    )
  }

  def randomLanguage(): Language = {
    Language.fromValue(faker.random().nextInt(1, 33))
  }

  def randomMeasurementSystem(): MeasurementSystem = {
    MeasurementSystem.fromValue(faker.random().nextInt(1, 2))
  }

  def randomWeather(measurementSystem: MeasurementSystem): Weather = {
    val temperature = randomTemperature(measurementSystem)
    Weather(
      faker.weather().description(),
      temperature,
      temperature - faker.random().nextInt(1, 10),
      temperature + faker.random().nextInt(1, 10),
      randomHumidity(),
      randomPressure(),
      randomWindSpeed(measurementSystem),
      randomWindDirection(),
      randomCloudCover(),
      measurementSystem,
      randomLanguage()
    )
  }

  def randomWeather(): Weather = {
    if (faker.random().nextBoolean()) randomWeather(IMPERIAL)
    else randomWeather(METRIC)
  }

  def randomCachedWeather(): CachedWeather = {
    val language = randomLanguage()
    val measurementSystem = randomMeasurementSystem()
    val localityKey = generateLocalityKey(randomPlace(), language, measurementSystem)
    CachedWeather(localityKey.get, Option(randomWeather(measurementSystem)), Option(randomTimestamp()))
  }

  private def randomTemperature(measurementSystem: MeasurementSystem): Float = {
    measurementSystem match {
      case IMPERIAL => faker.random().nextInt(-20, 117).floatValue()
      case _        => faker.random().nextInt(-7, 47).floatValue()
    }
  }

  private def randomHumidity(): Float = faker.random().nextInt(20, 100).floatValue()

  private def randomPressure(): Float = faker.random().nextInt(930, 1060).floatValue()

  private def randomWindSpeed(measurementSystem: MeasurementSystem): Float = {
    measurementSystem match {
      case IMPERIAL => faker.random().nextInt(0, 60).floatValue()
      case _        => faker.random().nextInt(0, 25).floatValue()
    }
  }

  private def randomWindDirection(): Float = faker.random().nextInt(0, 359).floatValue()

  private def randomCloudCover(): Float = faker.random().nextInt(0, 100).floatValue()
}
