name := "resty"

organization := "com.github.takezoe"

version := "0.0.14"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.json4s"                   %% "json4s-scalap"                    % "3.5.2",
  "com.netflix.hystrix"          %  "hystrix-core"                     % "1.5.12",
  "com.netflix.hystrix"          %  "hystrix-metrics-event-stream"     % "1.5.12",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"             % "2.8.8",
  "com.squareup.okhttp3"         %  "okhttp"                           % "3.9.1",
  "io.swagger"                   %  "swagger-models"                   % "1.5.13",
  "io.zipkin.brave"              %  "brave"                            % "4.10.0",
  "io.zipkin.brave"              %  "brave-instrumentation-okhttp3"    % "4.10.0",
  "io.zipkin.brave"              %  "brave-instrumentation-servlet"    % "4.10.0",
  "io.zipkin.reporter"           %  "zipkin-sender-okhttp3"            % "1.1.2",
  "commons-io"                   %  "commons-io"                       % "2.5",
  "org.webjars"                  %  "webjars-locator"                  % "0.32-1",
  "ch.qos.logback"               %  "logback-classic"                  % "1.2.3",
  "javax.servlet"                %  "javax.servlet-api"                % "3.1.0" % "provided",
  "org.scalatest"                %% "scalatest"                        % "3.0.3" % "test"
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
