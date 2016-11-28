name := "resty"

organization := "com.github.takezoe"

version := "0.0.1"

scalaVersion := "2.12.0"

val jettyVersion = "9.2.3.v20140905"

lazy val root = (project in file("."))
  .enablePlugins(JettyPlugin)
  .settings(
    scalaVersion := "2.12.0",
    libraryDependencies ++= Seq(
      "org.json4s"                   %% "json4s-scalap"                % "3.5.0",
      "com.netflix.hystrix"          %  "hystrix-core"                 % "1.5.8",
      "com.netflix.hystrix"          %  "hystrix-metrics-event-stream" % "1.5.8",
      "com.fasterxml.jackson.module" %% "jackson-module-scala"         % "2.8.4",
      "io.swagger"                   % "swagger-models"                % "1.5.10",
      "commons-io"                   % "commons-io"                    % "2.5",
      "javax.servlet"                %  "javax.servlet-api"            % "3.0.1" % "provided"
    ),
    scalacOptions := Seq("-deprecation"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
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
  )

lazy val sample = (project in file("sample"))
  .enablePlugins(JettyPlugin)
  .settings(
    name := "resty-sample",
    scalaVersion := "2.12.0",
    libraryDependencies ++= Seq(
      "org.eclipse.jetty"            %  "jetty-webapp"                 % jettyVersion % "container",
      "org.eclipse.jetty"            %  "jetty-plus"                   % jettyVersion % "container",
      "org.eclipse.jetty"            %  "jetty-annotations"            % jettyVersion % "container",
      "javax.servlet"                %  "javax.servlet-api"            % "3.0.1" % "provided"
    ),
    scalacOptions := Seq("-deprecation"),
    javaOptions in Jetty ++= Seq(
      "-Xdebug",
      "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
    )
  ).dependsOn(root)
