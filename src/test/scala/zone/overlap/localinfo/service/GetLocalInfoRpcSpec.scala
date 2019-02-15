// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo.service
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, Matchers}
import zone.overlap.localinfo.lib.weather.cache.{FoundationDbCache, NoCache}

class GetLocalInfoRpcSpec extends AsyncWordSpec with Matchers with AsyncMockFactory {

  "GetLocalInfoRpc configured with no cache" when {
    val getLocalInfoRpc = new GetLocalInfoRpc(NoCache)

    "" should {
      "" in {
        ???
      }
    }
  }

  "GetLocalInfoRpc configured with FoundationDB cache" when {
    class MockableFoundationDbCache extends FoundationDbCache(null, null, null, null)
    val fdbCache = mock[MockableFoundationDbCache]

    val getLocalInfoRpc = new GetLocalInfoRpc(fdbCache)

    "" should {
      "" in {
        ???
      }
    }
  }
}
