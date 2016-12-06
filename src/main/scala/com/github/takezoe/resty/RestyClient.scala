package com.github.takezoe.resty

import java.io.InputStream

import com.github.kristofa.brave.Brave
import com.github.kristofa.brave.httpclient.{BraveHttpRequestInterceptor, BraveHttpResponseInterceptor}
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.HttpClients
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender

object RestyClient {

  // TODO reporter should be configurable
  val sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans")
  val reporter = AsyncReporter.builder(sender).build()

  val brave = new Brave.Builder("brave-resty-example").reporter(reporter).build()
//  val brave = new Brave.Builder("brave-resty-example").build()

  val httpclient = HttpClients.custom()
    .addInterceptorFirst(BraveHttpRequestInterceptor.create(brave))
    .addInterceptorFirst(BraveHttpResponseInterceptor.create(brave))
    .build()

  // TODO More parameters
  def get[T](url: String, configurer: RequestBuilder => Unit = (builder) => ())(implicit deserializer: ResponseDeserializer[T]): T = {
    val builder = RequestBuilder.get(url)
    configurer(builder)
    val response = httpclient.execute(builder.build())
    if(response.getStatusLine.getStatusCode == 200){
      deserializer.deserialize(response.getEntity.getContent)
    } else {
      throw new RuntimeException(s"${url} responded status ${response.getStatusLine.getStatusCode}")
    }
  }


  trait ResponseDeserializer[T] {
    def deserialize(in: InputStream): T
  }

  implicit val StringResponseDeserializer = new ResponseDeserializer[String]{
    override def deserialize(in: InputStream): String = IOUtils.toString(in, "UTF-8")
  }
}

