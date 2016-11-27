name := "resty"

organization := "io.github.takezoe"

version := "1.0.0-SNAPSHOT"

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
    javaOptions in Jetty ++= Seq(
      "-Xdebug",
      "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000"
    )
  ).dependsOn(root)
