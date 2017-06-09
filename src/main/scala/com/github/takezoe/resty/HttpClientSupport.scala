package com.github.takezoe.resty

import java.util.concurrent.atomic.AtomicReference
import javax.servlet.ServletContextEvent

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.{JsonUtils, StringUtils}
import org.apache.commons.io.IOUtils
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender
import brave._
import brave.sampler._
import brave.httpclient._

import scala.reflect.ClassTag

object HttpClientSupport {

  private val _tracing = new AtomicReference[Tracing](null)
  private val _httpClient = new AtomicReference[CloseableHttpClient](null)

  def tracing: Tracing = {
    val instance = _tracing.get()
    if(instance == null){
      throw new IllegalStateException("HttpClientSupport has not been initialized or Zipkin support is disabled.")
    }
    instance
  }

  def httpClient = {
    val instance = _httpClient.get()
    if(instance == null){
      throw new IllegalStateException("HttpClientSupport has not been initialized yet.")
    }
    instance
  }

  def initialize(sce: ServletContextEvent): Unit = {
    if(_httpClient.get() != null){
      throw new IllegalArgumentException("HttpClientSupport has been already initialized.")
    }

    if("enable" == StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.ZipkinSupport))){
      val name = sce.getServletContext.getServletContextName
      val url  = StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.ZipkinServerUrl))
      val rate = StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.ZipkinSampleRate))
      val builder = Tracing.newBuilder().localServiceName(name)

      if(url.nonEmpty){
        val reporter = AsyncReporter.builder(OkHttpSender.create(url.trim)).build()
        builder.reporter(reporter)
      }

      if(rate.nonEmpty){
        val sampler = Sampler.create(rate.toFloat)
        builder.sampler(sampler)
      }

      _tracing.set(builder.build())
      _httpClient.set(TracingHttpClientBuilder.create(_tracing.get()).build())

    } else {
      _httpClient.set(HttpClients.createDefault())
    }
  }

  def shutdown(sce: ServletContextEvent): Unit = {
    if(_httpClient.get() == null){
      throw new IllegalArgumentException("HttpClientSupport is inactive now.")
    }
    _tracing.set(null)
    _httpClient.get().close()
    _httpClient.set(null)
  }

}

/**
 * HTTP client with Zipkin support.
 */
trait HttpClientSupport {

  def httpGet[T](url: String, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = RequestBuilder.get(url)
    execute(builder, configurer, c.runtimeClass)
  }

  def httpPost[T](url: String, params: Map[String, String], configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = RequestBuilder.post(url)
    params.foreach { case (key, value) => builder.addParameter(key, value) }
    execute(builder, configurer, c.runtimeClass)
  }

  def httpPostJson[T](url: String, doc: AnyRef, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = RequestBuilder.post(url)
    builder.setEntity(EntityBuilder.create()
      .setBinary(JsonUtils.serialize(doc).getBytes("UTF-8"))
      .setContentType(ContentType.APPLICATION_JSON)
      .build())
    execute(builder, configurer, c.runtimeClass)
  }

  def httpPut[T](url: String, params: Map[String, String], configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = RequestBuilder.put(url)
    params.foreach { case (key, value) => builder.addParameter(key, value) }
    execute(builder, configurer, c.runtimeClass)
  }

  def httpPutJson[T](url: String, doc: AnyRef, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = RequestBuilder.put(url)
    builder.setEntity(EntityBuilder.create()
      .setBinary(JsonUtils.serialize(doc).getBytes("UTF-8"))
      .setContentType(ContentType.APPLICATION_JSON)
      .build())
    execute(builder, configurer, c.runtimeClass)
  }

  def httpDelete[T](url: String, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = RequestBuilder.delete(url)
    execute(builder, configurer, c.runtimeClass)
  }

  protected def httpClient = HttpClientSupport.httpClient

  protected def execute[T](builder: RequestBuilder, configurer: RequestBuilder => Unit, clazz: Class[_]): Either[ErrorModel, T] = {
    configurer(builder)
    try {
      val response = httpClient.execute(builder.build())
      response.getStatusLine.getStatusCode match {
        case 200 => {
          val result = if (clazz == classOf[String]) {
            IOUtils.toString(response.getEntity.getContent, "UTF-8")
          } else {
            JsonUtils.deserialize(IOUtils.toString(response.getEntity.getContent, "UTF-8"), clazz)
          }
          Right(result.asInstanceOf[T])
        }
        case code =>
          Left(ErrorModel(Seq(s"${builder.getUri.toString} responded status ${response.getStatusLine.getStatusCode}")))
      }
    } catch {
      case e: Exception => Left(ErrorModel(Seq(e.toString)))
    }
  }

}



