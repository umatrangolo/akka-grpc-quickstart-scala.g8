resolvers += Resolver.sonatypeRepo("public")

evictionErrorLevel := Level.Info

addSbtPlugin("com.lightbend.akka.grpc" %  "sbt-akka-grpc"       % "$akka_grpc_version$")
addSbtPlugin("io.spray"                %  "sbt-revolver"        % "$sbt_revolver_version$")
addSbtPlugin("com.github.sbt"          %  "sbt-git"             % "$sbt_git_version$")
addSbtPlugin("com.github.sbt"          %  "sbt-native-packager" % "$sbt_native_packager_version$")
addSbtPlugin("org.scalameta"           %  "sbt-scalafmt"        % "$sbt_scalafmt_version$")
addSbtPlugin("nl.gn0s1s"               %% "sbt-dotenv"          % "$sbt_dotenv$")
addSbtPlugin("org.scoverage"           %  "sbt-scoverage"       % "$sbt_scoverage$")
addSbtPlugin("ch.epfl.scala"           %  "sbt-scalafix"        % "$sbt_scalafix_version$")

// ref: https://www.scala-sbt.org/1.x/docs/sbt-1.4-Release-Notes.html#sbt-dependency-graph+is+in-sourced
addDependencyTreePlugin
