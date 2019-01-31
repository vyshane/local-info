name := "local-info"
scalaVersion := "2.12.8"

enablePlugins(JavaAppPackaging)

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
resolvers += Resolver.bintrayRepo("vyshane", "maven")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  // Configuration
  "com.github.pureconfig" %% "pureconfig" % "0.10.1",
  // Dependency Injection
  "org.wvlet.airframe" %% "airframe" % "0.79",
  // Effects
  "io.monix" %% "monix" % "2.3.3",
  // gRPC and Protocol Buffers
  "io.grpc" % "grpc-netty-shaded" % "1.18.0",
  "io.grpc" % "grpc-stub" % "1.18.0",
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "beyondthelines" %% "grpcmonixruntime" % "0.0.7",
  // Monitoring
  "mu.node" %% "healthttpd" % "0.1.0",
  // Sunrise and sunset calculations
  "net.time4j" % "time4j-base" % "5.2",
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  // Testing
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "com.github.javafaker" % "javafaker" % "0.16" % Test
)

// Protobuf/gRPC code generation
PB.targets in Compile := Seq(
  scalapb.gen(grpc = true, flatPackage = true) -> (sourceManaged in Compile).value,
  grpcmonix.generators.GrpcMonixGenerator(flatPackage = true) -> (sourceManaged in Compile).value
)
PB.protoSources in Compile := Seq(
  sourceDirectory.value / "localinfo",
  sourceDirectory.value / "type"
)

// Code formatting
scalafmtConfig := file(".scalafmt.conf")
