name := "resty"

organization := "com.github.takezoe"

version := "0.0.17"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.json4s"                   %% "json4s-scalap"                    % "3.5.4",
  "com.netflix.hystrix"          %  "hystrix-core"                     % "1.5.12",
  "com.netflix.hystrix"          %  "hystrix-metrics-event-stream"     % "1.5.12",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"             % "2.9.5",
  "com.squareup.okhttp3"         %  "okhttp"                           % "3.10.0",
  "io.swagger"                   %  "swagger-models"                   % "1.5.19",
  "io.zipkin.brave"              %  "brave"                            % "4.19.2",
  "io.zipkin.brave"              %  "brave-instrumentation-okhttp3"    % "4.19.2",
  "io.zipkin.brave"              %  "brave-instrumentation-servlet"    % "4.19.2",
  "io.zipkin.reporter2"          %  "zipkin-sender-okhttp3"            % "2.6.0",
  "commons-io"                   %  "commons-io"                       % "2.6",
  "org.webjars"                  %  "webjars-locator"                  % "0.34",
  "ch.qos.logback"               %  "logback-classic"                  % "1.2.3",
  "javax.servlet"                %  "javax.servlet-api"                % "3.1.0" % "provided",
  "org.scalatest"                %% "scalatest"                        % "3.0.5" % "test"
)

scalacOptions := Seq("-deprecation")

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/takezoe/resty</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/takezoe/resty</url>
    <connection>scm:git:https://github.com/takezoe/resty.git</connection>
  </scm>
  <developers>
    <developer>
      <id>takezoe</id>
      <name>Naoki Takezoe</name>
      <email>takezoe_at_gmail.com</email>
      <timezone>+9</timezone>
    </developer>
  </developers>
)
