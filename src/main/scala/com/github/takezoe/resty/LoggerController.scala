package com.github.takezoe.resty

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.impl.StaticLoggerBinder

import scala.collection.JavaConverters._

class LoggerController {

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
