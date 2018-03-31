package com.github.takezoe.resty

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong, AtomicReference}
import javax.servlet.ServletContextEvent
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.StringUtils
import scala.collection.JavaConverters._

object CORSSupport {

  private val DefaultHeaders = Seq(
    "Cookie",
    "Host",
    "X-Forwarded-For",
    "Accept-Charset",
    "If-Modified-Since",
    "Accept-Language",
    "X-Forwarded-Port",
    "Connection",
    "X-Forwarded-Proto",
    "User-Agent",
    "Referer",
    "Accept-Encoding",
    "X-Requested-With",
    "Authorization",
    "Accept",
    "Content-Type"
  ).map(_.toUpperCase)

  private val SimpleHeaders: Seq[String] = Seq(
    "Origin",
    "Accept",
    "Accept-Language",
    "Content-Language"
  ).map(_.toUpperCase)

  private val SimpleContentTypes: Seq[String] = Seq(
    "application/x-www-form-urlencoded",
    "multipart/form-data",
    "text/plain"
  )

  private val enable = new AtomicBoolean(false)
  private val allowedOrigins = new AtomicReference[Seq[String]](Seq("*"))
  private val allowedMethods = new AtomicReference[Seq[String]](Seq("GET", "POST", "PUT", "DELETE"))
  private val allowedHeaders = new AtomicReference[Seq[String]](DefaultHeaders)
  private val preflightMaxAge = new AtomicLong(0)
  private val allowCredentials = new AtomicBoolean(false)

  def initialize(sce: ServletContextEvent): Unit = {
    if("enable" == StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.CORSSupport))){
      enable.set(true)

      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSAllowedOrigins)).foreach { value =>
        allowedOrigins.set(value.split(",").map(_.trim))
      }
      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSAllowedMethods)).foreach { value =>
        allowedMethods.set(value.split(",").map(_.trim.toUpperCase))
      }
      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSAllowedHeaders)).foreach { value =>
        allowedHeaders.set(value.split(",").map(_.trim.toUpperCase))
      }
      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSPreflightMaxAge)).foreach { value =>
        preflightMaxAge.set(StringUtils.trim(value).toLong)
      }
      Option(sce.getServletContext.getInitParameter(ConfigKeys.CORSAllowCredentials)).foreach { value =>
        allowCredentials.set(StringUtils.trim(value).toBoolean)
      }
    }
  }

  def processCORSRequest(request: HttpServletRequest): Option[CORSInfo] = {
    val isPreflight = request.getMethod == "OPTION" && request.getHeader("Access-Control-Request-Method") != null

    def _getAllowedOrigin: Option[String] =
      if(allowCredentials.get() == false && allowedOrigins.get.contains("*")){
        Some("*")
      } else if (allowedOrigins.get().contains(request.getHeader("Origin"))){
        Some(request.getHeader("Origin"))
      } else None

    def _getAllowedMethods: Option[Seq[String]] = {
      val method = if(isPreflight){
        request.getHeader("Access-Control-Request-Method")
      } else {
        request.getMethod
      }.toUpperCase

      if(allowedMethods.get().contains(method)){
        Some(allowedMethods.get())
      } else None
    }

    def _getAllowedHeaders: Option[Seq[String]] = {
      val headers = if(isPreflight){
        request.getHeader("Access-Control-Request-Headers").split(",").map(_.trim).toSeq
      } else {
        request.getHeaderNames.asScala
      }.map(_.toUpperCase)

      if(headers.forall { header =>
        SimpleHeaders.contains(header) ||
        (header == "CONTENT-TYPE" && SimpleContentTypes.contains(request.getContentType)) ||
        allowedHeaders.get().contains(header)
      }){
        Some(Option(request.getHeader("Access-Control-Request-Headers")).map(_.split(", ").map(_.trim).toSeq).getOrElse(Nil))
      } else None
    }

    if(enable.get() && request.getHeader("Origin") != null){
      for {
        origin  <- _getAllowedOrigin
        methods <- _getAllowedMethods
        headers <- _getAllowedHeaders
      } yield CORSInfo(origin, methods, headers, isPreflight)
    } else None
  }

  def setCORSResponseHeaders(response: HttpServletResponse, corsInfo: CORSInfo): Unit = {
    if(enable.get()){
      response.setHeader("Access-Control-Allow-Origin", corsInfo.allowedOrigin)
      if(allowCredentials.get()){
        response.setHeader("Access-Control-Allow-Credentials", "true")
      }
      if(corsInfo.isPreflight && preflightMaxAge.get() > 0){
        response.setHeader("Access-Control-Max-Age", preflightMaxAge.get().toString)
        if(corsInfo.allowedMethods.nonEmpty){
          response.setHeader("Access-Control-Allow-Methods", corsInfo.allowedMethods.mkString(", "))
        }
        if(corsInfo.allowedHeaders.nonEmpty){
          response.setHeader("Access-Control-Allow-Headers", corsInfo.allowedHeaders.mkString(", "))
        }
      }
    }
  }

  case class CORSInfo(
    allowedOrigin: String,
    allowedMethods: Seq[String],
    allowedHeaders: Seq[String],
    isPreflight: Boolean
  )

}
