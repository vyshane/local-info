// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.geolocation

import io.circe.{DecodingFailure, Json}
import monix.eval.Task
import zone.overlap.localinfo.lib.utils.Base62
import zone.overlap.localinfo.v1.local_info.Place
import zone.overlap.localinfo.lib.utils.TaskUtils._
import zone.overlap.protobuf.country_code.CountryCode

object NominatimAddressDecoder {

  def decodePlace(json: Json): Task[Place] = {
    val displayAddress: Task[String] = toTask(json.hcursor.get[String]("display_name"))

    val addressCursor = json.hcursor.downField("address")

    val addressKeys = addressCursor.keys
      .map(ks => ks.filter(k => k != "postcode" && k != "country_code" && k != "country"))
      .getOrElse(Iterable.empty)
      .toSeq
      // Reorder so that largest administrative area comes first
      .reverse

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
}
