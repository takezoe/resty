package com.github.takezoe.resty

import java.util.concurrent.atomic.AtomicReference
import javax.servlet.ServletContextEvent

import com.github.kristofa.brave.Brave
import com.github.kristofa.brave.httpclient.{BraveHttpRequestInterceptor, BraveHttpResponseInterceptor}
import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.JsonUtils
import org.apache.commons.io.IOUtils
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.reflect.ClassTag

object ZipkinSupport {

  private val _brave = new AtomicReference[Brave](null)
  private val _httpClient = new AtomicReference[CloseableHttpClient](null)

  def brave: Brave = {
    val instance = _brave.get()
    if(instance == null){
      throw new IllegalStateException("ZipkinSupport has not been initialized yet.")
    }
    instance
  }

  def httpClient = {
    val instance = _httpClient.get()
    if(instance == null){
      throw new IllegalStateException("ZipkinSupport has not been initialized yet.")
    }
    instance
  }

  def initialize(sce: ServletContextEvent): Unit = {
    if(_brave.get() != null){
      throw new IllegalArgumentException("ZipkinSupport has been already initialized.")
    }
    val name = sce.getServletContext.getServletContextName
    val url = sce.getServletContext.getInitParameter(ConfigKeys.ZipkinServerUrl)

    if(url == null || url.trim.isEmpty){
      _brave.set(new Brave.Builder(name).build())
    } else {
      val reporter = AsyncReporter.builder(OkHttpSender.create(url)).build()
      _brave.set(new Brave.Builder(name).reporter(reporter).build())
    }

    _httpClient.set(HttpClients.custom()
      .addInterceptorFirst(BraveHttpRequestInterceptor.create(ZipkinSupport.brave))
      .addInterceptorFirst(BraveHttpResponseInterceptor.create(ZipkinSupport.brave))
      .build())
  }

  def shutdown(sce: ServletContextEvent): Unit = {
    if(_brave.get() == null){
      throw new IllegalArgumentException("ZipkinSupport is inactive now.")
    }
    _brave.set(null)
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

  protected def httpClient = ZipkinSupport.httpClient

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


