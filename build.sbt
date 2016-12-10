name := "resty"

organization := "com.github.takezoe"

version := "0.0.5-SNAPSHOT"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "org.json4s"                   %% "json4s-scalap"                  % "3.5.0",
  "com.netflix.hystrix"          %  "hystrix-core"                   % "1.5.8",
  "com.netflix.hystrix"          %  "hystrix-metrics-event-stream"   % "1.5.8",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"           % "2.8.4",
  "io.swagger"                   %  "swagger-models"                 % "1.5.10",
  "io.zipkin.brave"              %  "brave-core"                     % "3.16.0",
  "io.zipkin.brave"              %  "brave-apache-http-interceptors" % "3.16.0",
  "io.zipkin.brave"              %  "brave-web-servlet-filter"       % "3.16.0",
  "io.zipkin.reporter"           %  "zipkin-sender-okhttp3"          % "0.6.9",
  "org.apache.httpcomponents"    %  "httpclient"                     % "4.5.2",
  "commons-io"                   %  "commons-io"                     % "2.5",
  "javax.servlet"                %  "javax.servlet-api"              % "3.0.1" % "provided",
  "org.scalatest"                %% "scalatest"                      % "3.0.1" % "test"
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
