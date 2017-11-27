package com.github.takezoe.resty.servlet

import java.util.EnumSet
import javax.servlet.{DispatcherType, ServletContextEvent, ServletContextListener}
import javax.servlet.annotation.WebListener

import com.github.takezoe.resty._
import com.github.takezoe.resty.util.StringUtils
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet

@WebListener
class InitializeListener extends ServletContextListener {

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    val context = sce.getServletContext

    // Initialize HttpClientSupport support
    HttpClientSupport.initialize(sce)
    if("enable" == StringUtils.trim(context.getInitParameter(ConfigKeys.ZipkinSupport))){
      context.addFilter("ZipkinBraveFilter", new ZipkinBraveFilter()).setAsyncSupported(true)
      context.getFilterRegistration("ZipkinBraveFilter")
        .addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/*")


    }

    // Initialize Swagger support
    if("enable" == StringUtils.trim(context.getInitParameter(ConfigKeys.SwaggerSupport))){
      Resty.register(new SwaggerController())
      context.addServlet("SwaggerUIServlet", new SwaggerUIServlet())
      context.getServletRegistration("SwaggerUIServlet").addMapping("/swagger-ui/*")
    }

    // Initialize Hystrix support
    HystrixSupport.initialize(sce)
    if("enable" == StringUtils.trim(context.getInitParameter(ConfigKeys.HystrixSupport))){
      context.addServlet("HystrixMetricsStreamServlet", new HystrixMetricsStreamServlet())
      context.getServletRegistration("HystrixMetricsStreamServlet").addMapping("/hystrix.stream")
    }

    if("enable" == StringUtils.trim(context.getInitParameter(ConfigKeys.WebJarsSupport))){
      val path = StringUtils.trim(context.getInitParameter(ConfigKeys.WebJarsPath))
      context.addServlet("WebJarsServlet", new WebJarsServlet())
      context.getServletRegistration("WebJarsServlet").addMapping(path)
    }
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {
    HttpClientSupport.shutdown(sce)
    HystrixSupport.shutdown(sce)
  }

}

object ConfigKeys {
  val ZipkinSupport     = "resty.zipkin"
  val ZipkinServerUrl   = "resty.zipkin.server.url"
  val ZipkinSampleRate  = "resty.zipkin.sample.rate"
  val ZipkinServiceName = "resty.zipkin.service.name"
  val SwaggerSupport    = "resty.swagger"
  val HystrixSupport    = "resty.hystrix"
  val WebJarsSupport    = "resty.webjars"
  val WebJarsPath       = "resty.webjars.path"
}
