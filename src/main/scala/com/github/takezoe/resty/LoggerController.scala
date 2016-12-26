package com.github.takezoe.resty

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.impl.StaticLoggerBinder

import scala.collection.JavaConverters._

class LoggerController {

  @Action(method = "GET", path = "/logger/levels")
  def levels(): Seq[LoggerModel] = {
    val factory = getLoggerContext()
    factory.getLoggerList.asScala.map { logger =>
      if(logger.getLevel == null){
        LoggerModel(logger.getName, "UNSPECIFIED")
      } else {
        LoggerModel(logger.getName, logger.getEffectiveLevel.levelStr)
      }
    }
  }

  @Action(method = "POST", path = "/logger/level")
  def setLevel(model: LoggerModel): Unit = {
    if(model.level != "UNSPECIFIED") {
      val logger = getLogger(model.name)
      logger.setLevel(
        model.level match {
          case "OFF"   => Level.OFF
          case "ERROR" => Level.ERROR
          case "WARN"  => Level.WARN
          case "INFO"  => Level.INFO
          case "DEBUG" => Level.DEBUG
          case "TRACE" => Level.TRACE
          case "ALL"   => Level.ALL
        }
      )
    }
  }

  @Action(method = "POST", path = "/logger/levels")
  def setLevels(model: LoggersModel): Unit = {
    model.loggers.foreach { model =>
      setLevel(model)
    }
    ()
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

case class LoggerModel(name: String, level: String)

case class LoggersModel(loggers: Seq[LoggerModel])