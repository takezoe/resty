package io.github.resty.sample

import javax.servlet.annotation.WebListener
import javax.servlet.{ServletContextEvent, ServletContextListener}

import io.github.resty._
import io.github.resty.model.AppInfo

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

class SampleController {

  @Action(method = "GET", path = "/hello/{name}")
  def hello(name: Option[String]): Message = {
    Message("Hello " + name + "!")
  }

  @Action(method = "POST", path = "/hello")
  def hello2(in: Hello): Message = {
    Message("Hello " + in.name + "!")
  }

  @Action(method = "GET", path = "/error", description="This operation always throws exception.")
  def error(): Unit = {
    throw new RuntimeException("test!!")
  }

}

case class Hello(name: Option[String], age: Int, message: Message, names: Seq[String])

case class Message(message: String)

