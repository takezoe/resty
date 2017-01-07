package com.github.takezoe.resty

import java.nio.file.{Files, Paths}

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.impl.StaticLoggerBinder

import scala.collection.JavaConverters._

class LoggerController(dir: String, file: String) {

  @Action(method = "GET", path = "/logger/levels")
  def levels(): Seq[LoggerModel] = {
    val factory = getLoggerContext()
    factory.getLoggerList.asScala.map { logger =>
      LoggerModel(logger.getName, Option(logger.getLevel).map(_.levelStr), logger.getEffectiveLevel.levelStr)
    }
  }

  @Action(method = "POST", path = "/logger/level")
  def setLevel(request: LogLevelUpdateRequest): Unit = {
    val logger = getLogger(request.name)

    request.level match {
      case Some(level) =>
        logger.setLevel(level match {
          case "OFF"   => Level.OFF
          case "ERROR" => Level.ERROR
          case "WARN"  => Level.WARN
          case "INFO"  => Level.INFO
          case "DEBUG" => Level.DEBUG
          case "TRACE" => Level.TRACE
          case "ALL"   => Level.ALL
        })
      case None =>
        logger.setLevel(null)
    }
  }

  @Action(method = "POST", path = "/logger/levels")
  def setLevels(requests: Array[LogLevelUpdateRequest]): Unit = {
    requests.foreach { request =>
      setLevel(request)
    }
  }

  @Action(method = "GET", path = "/logger/files")
  def getLogFiles(): Array[LogFileModel] = {
    Files.list(Paths.get(dir)).map[LogFileModel] { file =>
      LogFileModel(file.getFileName.toString, Files.size(file))
    }.toArray { length =>
      new Array[LogFileModel](length)
    }
  }

  @Action(method = "GET", path = "/logger/files/{name}")
  def downloadLogFile(name: String): java.io.File = {
    Paths.get(dir, name).toFile match {
      case file if !file.exists => NotFound()
      case file => file
    }
  }

  @Action(method = "GET", path = "/logger/tail")
  def tailLogFile(from: Int): Seq[String] = {
    val path = Paths.get(dir, file)
    if(Files.exists(path)){
      val stream = Files.lines(Paths.get("logs", "application.log"))
      stream.skip(from).toArray { length => new Array[String](length) }.toSeq
    } else {
      Seq.empty
    }
  }

  protected def getLoggerContext(): LoggerContext = {
    val factory = StaticLoggerBinder.getSingleton().getLoggerFactory()
    factory.asInstanceOf[LoggerContext]
  }

  protected def getLogger(name: String): ch.qos.logback.classic.Logger = {
    val factory = getLoggerContext()
    factory.getLogger(name)
  }

}

case class LoggerModel(name: String, level: Option[String], effectiveLevel: String)

case class LogLevelUpdateRequest(name: String, level: Option[String])

case class LogLevelUpdateRequests(loggers: Seq[LogLevelUpdateRequest])

case class LogFileModel(name: String, size: Long)