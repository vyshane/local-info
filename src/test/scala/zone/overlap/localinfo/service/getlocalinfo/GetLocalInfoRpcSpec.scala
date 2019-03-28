// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service.getlocalinfo

import java.time.{Clock, ZoneId}
import java.util.Optional
import io.grpc.{Status, StatusRuntimeException}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers}
import zone.overlap.localinfo.lib.geolocation.GeolocationClient
import zone.overlap.localinfo.lib.weather.WeatherClient
import zone.overlap.localinfo.lib.weather.cache.FoundationDbCache
import zone.overlap.localinfo.util.Fakes._
import zone.overlap.localinfo.v1.local_info.MeasurementSystem.METRIC
import zone.overlap.localinfo.v1.local_info.{GetLocalInfoRequest, LocalInfo}
import zone.overlap.protobuf.zoom_level.ZoomLevel

class GetLocalInfoRpcSpec extends AsyncWordSpec with Matchers with AsyncMockFactory {

  class MockableFoundationDbCache extends FoundationDbCache(null, Observable.never, null, null)

  val geolocationClient = mock[GeolocationClient]
  val weatherClient = mock[WeatherClient]
  val weatherCache = mock[MockableFoundationDbCache]
  val timeZoneQuery = mockFunction[Double, Double, Optional[ZoneId]]
  val clock = Clock.fixed(Clock.systemDefaultZone().instant(), ZoneId.systemDefault())
  val rpc = new GetLocalInfoRpc(geolocationClient, weatherClient, weatherCache, timeZoneQuery, clock)

  "GetLocalInfoRpc" when {
    "called with an invalid GetLocalInfoRequest" should {
      "return an invalid argument error status" in {
        recoverToExceptionIf[StatusRuntimeException] {
          rpc.handle(GetLocalInfoRequest()).runAsync
        } map { error =>
          error.getStatus.getCode shouldEqual Status.INVALID_ARGUMENT.getCode
          val errorMessage = s"INVALID_ARGUMENT: ${CoordinateIsRequired.errorMessage}; ${ZoomLevelIsRequired.errorMessage}"
          error.getMessage shouldEqual errorMessage
        }
      }
    }
    "called with zoom level smaller than the minimum zoom level that we can handle" should {
      "fallback to a zoom level that is within the valid range" in {
        val language = randomLanguage()
        (weatherClient.getCurrentWeather _).expects(*, *, *).returning(Task.now(randomWeather()))
        timeZoneQuery.expects(*, *).returning(Optional.of(ZoneId.of("UTC")))

        // Verify fallback to minimum that we handle
        (geolocationClient.getPlace _).expects(*, ZoomLevel.LEVEL_5, language).returning(Task.now(randomPlace()))

        rpc.handle(GetLocalInfoRequest(Some(randomCoordinate()), ZoomLevel.LEVEL_4, language)).runAsync map {
          _ shouldBe a[LocalInfo]
        }
      }
    }
    "called with zoom level greater than the maximum zoom level that we can handle" should {
      "fallback to a zoom level that is within the valid range" in {
        val language = randomLanguage()
        (weatherClient.getCurrentWeather _).expects(*, *, *).returning(Task.now(randomWeather()))
        timeZoneQuery.expects(*, *).returning(Optional.of(ZoneId.of("UTC")))

        // Verify fallback to maximum that we handle
        (geolocationClient.getPlace _).expects(*, ZoomLevel.LEVEL_14, language).returning(Task.now(randomPlace()))

        rpc.handle(GetLocalInfoRequest(Some(randomCoordinate()), ZoomLevel.LEVEL_15, language)).runAsync map (
          _ shouldBe a[LocalInfo]
        )
      }
    }
    "asked for a weather information that is in the cache" should {
      "not use the weather client to fetch the weather again" in {
        (geolocationClient.getPlace _).expects(*, *, *).returning(Task.now(randomPlace()))
        (weatherCache.get _).expects(*).returning(Task.now(Some(randomCachedWeather())))
        timeZoneQuery.expects(*, *).returning(Optional.of(ZoneId.of("UTC")))

        rpc
          .handle(GetLocalInfoRequest(Some(randomCoordinate()), randomZoomLevel(), randomLanguage(), METRIC))
          .runAsync map {
          _ shouldBe a[LocalInfo]
        }
      }
    }
    "asked for a weather information that is not in the cache" should {
      "use the weather client to fetch the weather and then cache it" in {
        val coordinate = randomCoordinate()
        val language = randomLanguage()
        val place = randomPlace()
        val localityKey = s"/${place.name}/${language.name}/METRIC"
        val weather = randomWeather(METRIC)
        (geolocationClient.getPlace _).expects(*, *, *).returning(Task.now(place))
        (weatherCache.get _).expects(localityKey).returning(Task.now(None))
        (weatherClient.getCurrentWeather _).expects(coordinate, language, METRIC).returning(Task.now(weather))
        (weatherCache.put _).expects(localityKey, weather).returning(Task.now(()))
        timeZoneQuery.expects(*, *).returning(Optional.of(ZoneId.of("UTC")))

        rpc.handle(GetLocalInfoRequest(Some(coordinate), randomZoomLevel(), language, METRIC)).runAsync map {
          _ shouldBe a[LocalInfo]
        }
      }
    }
    "asked to get local info" should {
      "run through the flow as expected" in (pending)
    }
  }
}
