package com.github.takezoe.resty

import java.io.InputStream

import com.github.kristofa.brave.Brave
import com.github.kristofa.brave.httpclient.{BraveHttpRequestInterceptor, BraveHttpResponseInterceptor}
import com.github.takezoe.resty.util.JsonUtils
import org.apache.commons.io.IOUtils
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.HttpClients
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

import scala.reflect.ClassTag

object RestyClient {

  // TODO reporter should be configurable
  protected val sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans")
  protected val reporter = AsyncReporter.builder(sender).build()

  val brave = new Brave.Builder("brave-resty-example").reporter(reporter).build()
//  val brave = new Brave.Builder("brave-resty-example").build()

  protected val httpclient = HttpClients.custom()
    .addInterceptorFirst(BraveHttpRequestInterceptor.create(brave))
    .addInterceptorFirst(BraveHttpResponseInterceptor.create(brave))
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
    val response = httpclient.execute(builder.build())
    response.getStatusLine.getStatusCode match {
      case 200 => IOUtils.toString(response.getEntity.getContent, "UTF-8")
      case code => throw new RuntimeException(s"${builder.getUri.toString} responded status ${response.getStatusLine.getStatusCode}")
    }
  }

  // TODO Return error as Either?
  protected def executeAsObject[T](builder: RequestBuilder, configurer: RequestBuilder => Unit, clazz: Class[_]): T = {
    configurer(builder)
    val response = httpclient.execute(builder.build())
    response.getStatusLine.getStatusCode match {
      case 200 => JsonUtils.deserialize(IOUtils.toString(response.getEntity.getContent, "UTF-8"), clazz).asInstanceOf[T]
      case code => throw new RuntimeException(s"${builder.getUri.toString} responded status ${response.getStatusLine.getStatusCode}")
    }
  }

}



