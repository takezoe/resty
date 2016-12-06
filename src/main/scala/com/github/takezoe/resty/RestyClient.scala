package com.github.takezoe.resty

import java.util.concurrent.atomic.AtomicReference
import javax.servlet.ServletContextEvent

import com.github.kristofa.brave.Brave
import com.github.kristofa.brave.httpclient.{BraveHttpRequestInterceptor, BraveHttpResponseInterceptor}
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

  def brave: Brave = {
    val instance = _brave.get()
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
    val url = sce.getServletContext.getInitParameter("resty.zipkin.server.url")
    if(url == null || url.trim.isEmpty){
      _brave.set(new Brave.Builder(name).build())
    } else {
      val reporter = AsyncReporter.builder(OkHttpSender.create(url)).build()
      _brave.set(new Brave.Builder(name).reporter(reporter).build())
    }
  }

  def shutdown(sce: ServletContextEvent): Unit = {
    if(_brave.get() == null){
      throw new IllegalArgumentException("ZipkinSupport is inactive now.")
    }
    _brave.set(null)
  }

}

/**
 * HTTP client with Zipkin support.
 */
object RestyClient {

  protected val httpClient = HttpClients.custom()
    .addInterceptorFirst(BraveHttpRequestInterceptor.create(ZipkinSupport.brave))
    .addInterceptorFirst(BraveHttpResponseInterceptor.create(ZipkinSupport.brave))
    .build()

  def get[T](url: String, configurer: RequestBuilder => Unit = (builder) => ()): String = {
    val builder = RequestBuilder.get(url)
    executeAsString(builder, configurer)
  }

  def getJson[T](url: String, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): T = {
    val builder = RequestBuilder.get(url)
    executeAsObject(builder, configurer, c.runtimeClass)
  }

  def post[T](url: String, params: Map[String, String], configurer: RequestBuilder => Unit = (builder) => ()): String = {
    val builder = RequestBuilder.post(url)
    params.foreach { case (key, value) => builder.addParameter(key, value) }
    executeAsString(builder, configurer)
  }

  def postJson[T](url: String, body: AnyRef, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): T = {
    val builder = RequestBuilder.post(url)
    builder.setEntity(EntityBuilder.create()
      .setBinary(JsonUtils.serialize(body).getBytes("UTF-8"))
      .setContentType(ContentType.APPLICATION_JSON)
      .build())
    executeAsObject(builder, configurer, c.runtimeClass)
  }

  def put[T](url: String, params: Map[String, String], configurer: RequestBuilder => Unit = (builder) => ()): String = {
    val builder = RequestBuilder.put(url)
    params.foreach { case (key, value) => builder.addParameter(key, value) }
    executeAsString(builder, configurer)
  }

  def putJson[T](url: String, body: AnyRef, configurer: RequestBuilder => Unit = (builder) => ())(implicit c: ClassTag[T]): T = {
    val builder = RequestBuilder.put(url)
    builder.setEntity(EntityBuilder.create()
      .setBinary(JsonUtils.serialize(body).getBytes("UTF-8"))
      .setContentType(ContentType.APPLICATION_JSON)
      .build())
    executeAsObject(builder, configurer, c.runtimeClass)
  }

  def delete[T](url: String, configurer: RequestBuilder => Unit = (builder) => ()): String = {
    val builder = RequestBuilder.delete(url)
    executeAsString(builder, configurer)
  }

  // TODO Return error as Either?
  protected def executeAsString(builder: RequestBuilder, configurer: RequestBuilder => Unit): String = {
    configurer(builder)
    val response = httpClient.execute(builder.build())
    response.getStatusLine.getStatusCode match {
      case 200 => IOUtils.toString(response.getEntity.getContent, "UTF-8")
      case code => throw new RuntimeException(s"${builder.getUri.toString} responded status ${response.getStatusLine.getStatusCode}")
    }
  }

  // TODO Return error as Either?
  protected def executeAsObject[T](builder: RequestBuilder, configurer: RequestBuilder => Unit, clazz: Class[_]): T = {
    configurer(builder)
    val response = httpClient.execute(builder.build())
    response.getStatusLine.getStatusCode match {
      case 200 => JsonUtils.deserialize(IOUtils.toString(response.getEntity.getContent, "UTF-8"), clazz).asInstanceOf[T]
      case code => throw new RuntimeException(s"${builder.getUri.toString} responded status ${response.getStatusLine.getStatusCode}")
    }
  }

}



