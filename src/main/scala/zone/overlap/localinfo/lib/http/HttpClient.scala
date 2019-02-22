// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.http

import com.softwaremill.sttp._
import com.softwaremill.sttp.{HttpURLConnectionBackend, Uri}
import io.circe.{Json, ParsingFailure}
import io.circe.parser._
import monix.eval.Task
import zone.overlap.localinfo.lib.errors.Internal
import zone.overlap.localinfo.lib.utils.TaskUtils._

object HttpClient {

  implicit val backend = HttpURLConnectionBackend()

  def httpGetJson(uri: Uri): Task[Json] = {
    for {
      e <- Task(sttp.get(uri).send().body)
      b <- fromEither[String, String](errorMessage => Internal(errorMessage).exception)(e)
      json <- parseJson(b)
    } yield json
  }

  private def parseJson(body: String): Task[Json] = {
    val json = parse(body)
    fromEither[ParsingFailure, Json](p => Internal(p.message).exception)(json)
  }
}
