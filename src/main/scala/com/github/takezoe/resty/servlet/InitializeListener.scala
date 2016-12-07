package com.github.takezoe.resty.servlet

import java.util.EnumSet
import javax.servlet.{DispatcherType, ServletContextEvent, ServletContextListener}
import javax.servlet.annotation.WebListener

import com.github.takezoe.resty.{HystrixSupport, Resty, SwaggerController, ZipkinSupport}
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet

@WebListener
class InitializeListener extends ServletContextListener {

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    val context = sce.getServletContext

    // Initialize Zipkin support
    ZipkinSupport.initialize(sce)
    if("enable" == context.getInitParameter(ConfigKeys.ZipkinSupport)) {
      context.addFilter("ZipkinBraveFilter", new ZipkinBraveFilter())
      context.getFilterRegistration("ZipkinBraveFilter").addMappingForUrlPatterns(EnumSet.allOf(classOf[DispatcherType]), true, "/*")
    }

    // Initialize Swagger support
    if("enable" == context.getInitParameter(ConfigKeys.SwaggerSupport)){
      Resty.register(new SwaggerController())
      context.addServlet("SwaggerUIServlet", new SwaggerUIServlet())
      context.getServletRegistration("SwaggerUIServlet").addMapping("/swagger-ui/*")
    }

    // Initialize Hystrix support
    HystrixSupport.initialize(sce)
    if("enable" == context.getInitParameter(ConfigKeys.HystrixSupport)){
      context.addServlet("HystrixMetricsStreamServlet", new HystrixMetricsStreamServlet())
      context.getServletRegistration("HystrixMetricsStreamServlet").addMapping("/hystrix.stream")
    }
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {
    ZipkinSupport.shutdown(sce)
    HystrixSupport.shutdown(sce)
  }

}

object ConfigKeys {
  val ZipkinSupport   = "resty.zipkin"
  val ZipkinServerUrl = "resty.zipkin.server.url"
  val SwaggerSupport  = "resty.swagger"
  val HystrixSupport  = "resty.hystrix"
}
