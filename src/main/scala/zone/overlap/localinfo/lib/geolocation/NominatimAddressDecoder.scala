// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import io.circe.{ACursor, DecodingFailure, Json}
import monix.eval.Task
import zone.overlap.localinfo.lib.errors.Internal
import zone.overlap.localinfo.lib.utils.Base62
import zone.overlap.localinfo.v1.local_info.Place
import zone.overlap.localinfo.lib.utils.TaskUtils._
import zone.overlap.protobuf.country_code.CountryCode

object NominatimAddressDecoder {

  def decodePlace(json: Json): Task[Place] = {
    val displayAddress: Task[String] = toTask(json.hcursor.get[String]("display_name"))

    val addressCursor = json.hcursor.downField("address")
    val addressKeys = addressFields(addressCursor)

    if (addressKeys.size == 0)
      return Task.raiseError(Internal("Unable to decode address for Place").exception)

    val addressParts: Seq[Task[String]] = addressKeys
      .map(k => addressCursor.get[String](k))
      .map(toTask)

    val displayName: Task[String] = addressParts.last

    val country: Task[String] = toTask(addressCursor.get[String]("country"))

    val countryCode: Task[CountryCode] = toTask(
      addressCursor
        .get[String]("country_code")
        .map(_.toUpperCase)
        .map(CountryCode.fromName)
        .flatMap { c =>
          if (c.isDefined) Right(c.get)
          else Left(DecodingFailure("Unknown country code", List()))
        }
    )

    val resourceName: Task[String] = {
      val resourceNameSuffix: Task[String] = Task
        .sequence(addressParts)
        .map(ps => ps.map(p => Base62.encode(p)).mkString("."))

      Task.zipMap2(countryCode, resourceNameSuffix)((c, n) => s"$c.$n")
    }

    for {
      n <- resourceName
      cc <- countryCode
      d <- displayName
      a <- displayAddress
      c <- country
    } yield Place(n, cc, d, a, c)
  }

  private def addressFields(addressCursor: ACursor): Seq[String] = {
    val addressKeys = addressCursor.keys
      .map(ks => ks.filter(k => k != "postcode" && k != "country_code"))
      .getOrElse(Iterable.empty)
      .toSeq
      // Reorder so that largest administrative area comes first
      .reverse

    // Ensure that we will have at least one key
    if (addressKeys.size > 1) addressKeys.filterNot(_ == "country")
    else addressKeys
  }
}
