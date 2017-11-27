package com.github.takezoe.resty

import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import javax.servlet.ServletContextEvent

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.{JsonUtils, StringUtils}
import zipkin.reporter.AsyncReporter
import zipkin.reporter.okhttp3.OkHttpSender
import brave._
import brave.sampler._
import _root_.okhttp3._
import brave.okhttp3.TracingInterceptor

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag

object HttpClientSupport {

  val ContentType_JSON = MediaType.parse("application/json; charset=utf-8")

  private val _tracing = new AtomicReference[Tracing](null)
  private val _httpClient = new AtomicReference[OkHttpClient](null)

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
      val name = StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.ZipkinServiceName))
      val url  = StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.ZipkinServerUrl))
      val rate = StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.ZipkinSampleRate))
      val builder = Tracing.newBuilder().localServiceName(if(name.nonEmpty) name else InetAddress.getLocalHost.getHostAddress)

      if(url.nonEmpty){
        val reporter = AsyncReporter.builder(OkHttpSender.create(url.trim)).build()
        builder.reporter(reporter)
      }

      if(rate.nonEmpty){
        val sampler = Sampler.create(rate.toFloat)
        builder.sampler(sampler)
      }

      val httpTracing = builder.build()

      _tracing.set(httpTracing)

      val client = new OkHttpClient.Builder().dispatcher(new Dispatcher(
          tracing.currentTraceContext().executorService(new Dispatcher().executorService())
        ))
        .addNetworkInterceptor(TracingInterceptor.create(httpTracing))
        .build()

      _httpClient.set(client)

    } else {
      _httpClient.set(new OkHttpClient())
    }
  }

  def shutdown(sce: ServletContextEvent): Unit = {
    if(_httpClient.get() == null){
      throw new IllegalArgumentException("HttpClientSupport is inactive now.")
    }
    _tracing.set(null)
    _httpClient.get().dispatcher().executorService().shutdown()
    _httpClient.set(null)
  }

}

/**
 * HTTP client with Zipkin support.
 */
trait HttpClientSupport {

  def httpGet[T](url: String, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(url).get()

    execute(builder, configurer, c.runtimeClass)
  }

  def httpGetAsync[T](url: String, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(url).get()

    executeAsync(builder, configurer, c.runtimeClass)
  }

  def httpPost[T](url: String, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(url).post(formBuilder.build())

    execute(builder, configurer, c.runtimeClass)
  }

  def httpPostAsync[T](url: String, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(url).post(formBuilder.build())

    executeAsync(builder, configurer, c.runtimeClass)
  }

  def httpPostJson[T](url: String, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(url)
      .post(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    execute(builder, configurer, c.runtimeClass)
  }

  def httpPostJsonAsync[T](url: String, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(url)
      .post(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    executeAsync(builder, configurer, c.runtimeClass)
  }

  def httpPut[T](url: String, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(url).put(formBuilder.build())

    execute(builder, configurer, c.runtimeClass)
  }

  def httpPutAsync[T](url: String, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(url).put(formBuilder.build())

    executeAsync(builder, configurer, c.runtimeClass)
  }

  def httpPutJson[T](url: String, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(url)
      .put(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    execute(builder, configurer, c.runtimeClass)
  }

  def httpPutJsonAsync[T](url: String, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(url)
      .put(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    executeAsync(builder, configurer, c.runtimeClass)
  }

  def httpDelete[T](url: String, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(url).delete()

    execute(builder, configurer, c.runtimeClass)
  }

  def httpDeleteAsync[T](url: String, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(url).delete()

    executeAsync(builder, configurer, c.runtimeClass)
  }

  protected def httpClient = HttpClientSupport.httpClient

  protected def execute[T](builder: Request.Builder, configurer: Request.Builder => Unit, clazz: Class[_]): Either[ErrorModel, T] = {
    try {
      configurer(builder)

      val request = builder.build()
      val response = httpClient.newCall(request).execute()

      handleResponse(request, response, clazz)
      
    } catch {
      case e: Exception => Left(ErrorModel(Seq(e.toString)))
    }
  }

  protected def executeAsync[T](builder: Request.Builder, configurer: Request.Builder => Unit, clazz: Class[_]): Future[Either[ErrorModel, T]] = {
    try {
      configurer(builder)

      val request = builder.build()
      val promise = Promise[Either[ErrorModel, T]]()

      httpClient.newCall(request).enqueue(new Callback {
        override def onFailure(call: Call, e: IOException): Unit = {
          promise.failure(e)
        }
        override def onResponse(call: Call, response: Response): Unit = {
          promise.success(handleResponse(request, response, clazz))
        }
      })

      promise.future

    } catch {
      case e: Exception => Future.successful(Left(ErrorModel(Seq(e.toString))))
    }
  }

  protected def handleResponse[T](request: Request, response: Response, clazz: Class[_]): Either[ErrorModel, T] = {
    response.code match {
      case 200 => {
        val result = if (clazz == classOf[String]) {
          new String(response.body.bytes, StandardCharsets.UTF_8)
        } else {
          JsonUtils.deserialize(new String(response.body.bytes, StandardCharsets.UTF_8), clazz)
        }
        Right(result.asInstanceOf[T])
      }
      case code =>
        Left(ErrorModel(Seq(s"${request.url.toString} responded status ${code}")))
    }
  }


}



