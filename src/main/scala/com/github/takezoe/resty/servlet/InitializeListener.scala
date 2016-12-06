package com.github.takezoe.resty.servlet

import javax.servlet.{ServletContextEvent, ServletContextListener}
import javax.servlet.annotation.WebListener

import com.github.takezoe.resty.ZipkinSupport

@WebListener
class InitializeListener extends ServletContextListener {

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    ZipkinSupport.initialize(sce)
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {
    ZipkinSupport.shutdown(sce)
  }

}
