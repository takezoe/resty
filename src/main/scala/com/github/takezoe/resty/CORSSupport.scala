package com.github.takezoe.resty

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong, AtomicReference}
import javax.servlet.ServletContextEvent
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.StringUtils

object CORSSupport {

  private val enable = new AtomicBoolean(false)
  private val allowedOrigins = new AtomicReference[Seq[String]]()
  private val preflightMaxAge = new AtomicLong(0)
  private val allowCredentials = new AtomicBoolean(false)

  def initialize(sce: ServletContextEvent): Unit = {
    if("enable" == StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.CORSSupport))){
      enable.set(true)

      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSAllowedOrigins)) match {
        case Some(value) => allowedOrigins.set(value.split(",").map(_.trim))
        case None        => allowedOrigins.set(Seq("*"))
      }

      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSPreflightMaxAge)).foreach { value =>
        preflightMaxAge.set(StringUtils.trim(value).toLong)
      }

      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSAllowCredentials)).foreach { value =>
        allowCredentials.set(StringUtils.trim(value).toBoolean)
      }
    }
  }

  def isCORSRequest(request: HttpServletRequest): Boolean = {
    enable.get() && request.getHeader("Origin") != null
  }

  def getAllowedOrigin(request: HttpServletRequest): Option[String] = {
    if(enable.get()){
      if(allowCredentials.get() == false && allowedOrigins.get.contains("*")){
        Some("*")
      } else if (allowedOrigins.get().contains(request.getHeader("Origin"))){
        Some(request.getHeader("Origin"))
      } else None
    } else None
  }

  def isPreflightRequest(request: HttpServletRequest): Boolean =
    enable.get() && request.getMethod == "OPTION" && request.getHeader("Access-Control-Request-Method") != null

  def setCORSResponseHeaders(response: HttpServletResponse, preflight: Boolean, origin: String): Unit = {
    if(enable.get()){
      response.setHeader("Access-Control-Allow-Origin", origin)
      if(allowCredentials.get()){
        response.setHeader("Access-Control-Allow-Credentials", "true")
      }
      if(preflight && preflightMaxAge.get() > 0){
        response.setHeader("Access-Control-Max-Age", preflightMaxAge.get().toString)
      }
    }
  }

}
