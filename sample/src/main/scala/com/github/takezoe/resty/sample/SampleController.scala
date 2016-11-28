package com.github.takezoe.resty.sample

import javax.servlet.annotation.WebListener
import javax.servlet.{ServletContextEvent, ServletContextListener}

import com.github.takezoe.resty._
import com.github.takezoe.resty.model.AppInfo

@WebListener
class InitializeListener extends ServletContextListener {

  override def contextDestroyed(sce: ServletContextEvent): Unit = {
  }

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    Resty.register(AppInfo(
      title = sce.getServletContext.getServletContextName,
      description = "Sample application of Resty framework"
    ))
    Resty.register(new SampleController())
  }

}

@Controller(name = "sample", description = "Sample APIs")
class SampleController {

  @Action(method = "GET", path = "/hello/{name}")
  def hello(name: String): Message = {
    Message(s"Hello ${name}!")
  }

  @Action(method = "GET", path = "/hello")
  def helloSeq(names: Seq[String]): Seq[String] = {
    names.map { name =>
      s"Hello ${name}!"
    }
  }

  @Action(method = "POST", path = "/hello")
  def hello2(in: Hello): Message = {
    Message(s"Hello ${in.name.getOrElse("World")}!")
  }

  @Action(method = "GET", path = "/error", description="This operation always throws exception.", deprecated = true)
  @deprecated
  def error(): Unit = {
    throw new RuntimeException("test!!")
  }

}

case class Hello(name: Option[String])

case class Message(message: String)

