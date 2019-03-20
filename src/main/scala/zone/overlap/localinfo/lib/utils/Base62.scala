// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.lib.utils

import java.nio.charset.StandardCharsets
import io.seruco.encoding.base62.{Base62 => B62}

object Base62 {
  lazy val base62 = B62.createInstance()

  def encode(value: String): String = {
    new String(base62.encode(value.getBytes(StandardCharsets.UTF_8)))
  }

  def decode(value: String): String = {
    new String(base62.decode(value.getBytes(StandardCharsets.UTF_8)))
  }
}
