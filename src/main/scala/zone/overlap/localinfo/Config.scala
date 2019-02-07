// Copyright 2019 Vy-Shane Xie

package zone.overlap.localinfo

case class Config(
    grpcPort: Int,
    statusPort: Int,
    locationIqToken: String,
    openWeatherMapApiKey: String,
    fdbClusterFile: String,
    fdbKeySpaceDirectory: String
)
