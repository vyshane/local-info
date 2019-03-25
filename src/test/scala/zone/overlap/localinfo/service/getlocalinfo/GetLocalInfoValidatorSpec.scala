// Copyright 2019 Vy-Shane Xie Sin Fat

package zone.overlap.localinfo.service.getlocalinfo

import cats.data.Chain
import cats.data.Validated.Invalid
import cats.implicits._
import org.scalatest.{Matchers, WordSpec}
import zone.overlap.localinfo.service.getlocalinfo.GetLocalInfoValidator._
import zone.overlap.localinfo.v1.local_info.GetLocalInfoRequest
import zone.overlap.protobuf.coordinate.Coordinate
import zone.overlap.protobuf.zoom_level.ZoomLevel

class GetLocalInfoValidatorSpec extends WordSpec with Matchers {

  "GetLocalInfoValidator" when {
    "validating an GetLocalInfoRequest with empty coordinate and empty zoom level" should {
      "return all the validation errors" in {
        validate(GetLocalInfoRequest()).toString shouldEqual
          Invalid(Chain(CoordinateIsRequired, ZoomLevelIsRequired)).toString
      }
    }
    "validating an GetLocalInfoRequest with invalid coordinate and empty zoom level" should {
      "return all the validation errors" in {
        val coordinateLessThanMin = Some(Coordinate(-91, -181))
        validate(GetLocalInfoRequest(coordinateLessThanMin)).toString shouldEqual
          Invalid(Chain(LatitudeOutOfRange, LongitudeOutOfRange, ZoomLevelIsRequired)).toString

        val coordinateGreaterThanMax = Some(Coordinate(91, 181))
        validate(GetLocalInfoRequest(coordinateGreaterThanMax)).toString shouldEqual
          Invalid(Chain(LatitudeOutOfRange, LongitudeOutOfRange, ZoomLevelIsRequired)).toString
      }
    }
    "validating a valid GetLocalInfoRequest" should {
      "return the request" in {
        val request = GetLocalInfoRequest(Some(Coordinate(-20.26381, 57.4791)), ZoomLevel.LEVEL_12)
        validate(request) shouldEqual request.validNec
      }
    }
  }
}
