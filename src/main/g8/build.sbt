lazy val akkaHttpVersion      = "$akka_http_version$"
lazy val akkaGrpcVersion      = "$akka_grpc_version$"
lazy val akkaVersion          = "$akka_version$"
lazy val catsCoreVersion      = "$cats_core_version$"
lazy val logbackVersion       = "$logback_version$"
lazy val logstashVersion      = "$logstash_version$"
lazy val scalaTestVersion     = "$scala_test_version$"
lazy val scalaTestPlusVersion = "$scala_test_plus_version$"
lazy val mockitoVersion       = "$mockito_version$"
lazy val scalaCheckVersion    = "$scala_check_version$"
lazy val fasterXmlVersion     = "$faster_xml_version$"
lazy val orgImportVersion     = "$organize_import_version$"
lazy val gRPCJavaVersion      = "$grpc_java_version$"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "$organization$",
      scalaVersion    := "$scala_version$",
      semanticdbEnabled := true,
      semanticdbVersion := scalafixSemanticdb.revision,
      scalafixDependencies += "com.github.liancheng" %% "organize-imports" % orgImportVersion,
      fork := true
    )),
    name := "$name$",
    javacOptions ++= Seq("-source", "11", "-target", "11", "-Xlint"),
    (Docker / maintainer) := "$docker_maintainer$",
    (Docker / maintainer) := "$docker_package_name$",
    dockerExposedPorts ++= Seq(9000),
    dockerBaseImage := "$docker_base_image$",
    git.formattedShaVersion := git.gitHeadCommit.value map { sha => s"\$sha".take(7) },
    coverageMinimum := 60,
    coverageFailOnMinimum := true,
    coverageExcludedPackages := "<empty>;.*Service.*",
    // Avoid to generate Scaladocs
    (Compile / doc / sources) := Seq.empty,
    (Compile / packageDoc / publishArtifact) := false,
    libraryDependencies ++= Seq(
      // The Akka HTTP overwrites are required because Akka-gRPC depends on 10.1.x
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,

      "com.typesafe.akka"            %% "akka-actor-typed"          % akkaVersion,
      "com.typesafe.akka"            %% "akka-stream"               % akkaVersion,
      "com.typesafe.akka"            %% "akka-discovery"            % akkaVersion,
      "com.typesafe.akka"            %% "akka-pki"                  % akkaVersion,
      "ch.qos.logback"                % "logback-classic"           % logbackVersion,
      "net.logstash.logback"          % "logstash-logback-encoder"  % logstashVersion,
      "org.typelevel"                %% "cats-core"                 % catsCoreVersion,

      // gRPC utils
      "io.grpc" % "grpc-services" % gRPCJavaVersion,

      "com.typesafe.akka"            %% "akka-actor-testkit-typed"  % akkaVersion            % Test,
      "com.typesafe.akka"            %% "akka-stream-testkit"       % akkaVersion            % Test,
      "org.mockito"                   % "mockito-core"              % mockitoVersion         % Test,
      "org.scalacheck"               %% "scalacheck"                % scalaCheckVersion      % Test,
      "org.scalatest"                %% "scalatest"                 % scalaTestVersion       % Test,
      "org.scalatestplus"            %% "mockito-3-4"               % scalaTestPlusVersion   % Test,

      // Only needed to json marshal objects into logging directives
      "com.fasterxml.jackson.module"   %% "jackson-module-scala"    % fasterXmlVersion,
      "com.fasterxml.jackson.datatype" %  "jackson-datatype-jsr310" % fasterXmlVersion
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-unchecked",
      "-deprecation",
      "-Wdead-code",
      "-Wnumeric-widen",
      "-Wvalue-discard",
      "-Xfatal-warnings",
      "-Xlint:unused",
      "-Xlint:nonlocal-return",
      "-Xlint:deprecation",
      "-Wconf:src=src_managed/.*:s"
    )
  )
  .enablePlugins(GitVersioning)
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  // Required to use 'sh' instead of `bash` with Alpine dist
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AkkaGrpcPlugin)
