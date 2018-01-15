package com.github.takezoe.resty

import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import javax.servlet.ServletContextEvent

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.{JsonUtils, StringUtils}
import zipkin2.reporter.AsyncReporter
import brave._
import brave.sampler._
import _root_.okhttp3._
import brave.okhttp3.TracingInterceptor
import zipkin2.reporter.okhttp3.OkHttpSender

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
        builder.spanReporter(reporter)
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

trait RequestExecutor {

  def url: String

  def execute[T](httpClient: OkHttpClient, builder: Request.Builder,
                 configurer: Request.Builder => Unit, clazz: Class[_]): Either[ErrorModel, T]

  def executeAsync[T](httpClient: OkHttpClient, builder: Request.Builder,
                      configurer: Request.Builder => Unit, clazz: Class[_]): Future[Either[ErrorModel, T]]


  protected def handleResponse[T](request: Request, response: Response, clazz: Class[_]): Either[ErrorModel, T] = {
    try {
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
    } finally {
      response.close()
    }
  }

}

class SimpleRequestExecutor(val url: String, config: HttpExecutorConfig) extends RequestExecutor {

  @volatile private var disabledTime: Option[Long] = None

  def execute[T](httpClient: OkHttpClient, builder: Request.Builder,
                 configurer: Request.Builder => Unit, clazz: Class[_]): Either[ErrorModel, T] = {
    try {
      checkWhetherEnabled()

      configurer(builder)

      val request = builder.build()
      val response = withRetry(httpClient, request, config)

      handleResponse(request, response, clazz)

    } catch {
      case e: Exception => Left(ErrorModel(Seq(e.toString)))
    }
  }

  private def checkWhetherEnabled(): Unit = {
    disabledTime match {
      case None => ()
      case Some(time) if time <= System.currentTimeMillis - config.resetInterval => disabledTime = None
      case Some(_) => throw new RuntimeException(s"${url} is not available now.")
    }
  }

  private def withRetry(httpClient: OkHttpClient, request: Request, config: HttpExecutorConfig): Response = {
    var count = 0
    while(true){
      try {
        return httpClient.newCall(request).execute()
      } catch {
        case _: Exception if count < config.maxRetry => {
          count = count + 1
          Thread.sleep(config.retryInterval)
        }
        case e: Exception => {
          disabledTime = Some(System.currentTimeMillis)
          throw e
        }
      }
    }
    ???
  }

  def executeAsync[T](httpClient: OkHttpClient, builder: Request.Builder,
                      configurer: Request.Builder => Unit, clazz: Class[_]): Future[Either[ErrorModel, T]] = {
    try {
      checkWhetherEnabled()

      configurer(builder)

      val request = builder.build()
      val promise = Promise[Either[ErrorModel, T]]()

      httpClient.newCall(request).enqueue(new Callback {
        var count = 0
        override def onFailure(call: Call, e: IOException): Unit = {
          if(count < config.maxRetry){
            count = count + 1
            Thread.sleep(config.retryInterval) // TODO Don't brock a thread here!
            httpClient.newCall(request)
          } else {
            disabledTime = Some(System.currentTimeMillis)
            promise.failure(e)
          }
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
}

case class HttpExecutorConfig(maxRetry: Int = 1, retryInterval: Int = 0, resetInterval: Int = 0)

/**
 * HTTP client with Zipkin support.
 */
trait HttpClientSupport {

  implicit def string2target(url: String): SimpleRequestExecutor = new SimpleRequestExecutor(url, HttpExecutorConfig())

  protected def httpClient = HttpClientSupport.httpClient

  def httpGet[T](executor: RequestExecutor, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(executor.url).get()
    executor.execute(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpGetAsync[T](executor: RequestExecutor, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(executor.url).get()
    executor.executeAsync(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPost[T](executor: RequestExecutor, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(executor.url).post(formBuilder.build())
    executor.execute(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPostAsync[T](executor: RequestExecutor, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(executor.url).post(formBuilder.build())
    executor.executeAsync(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPostJson[T](executor: RequestExecutor, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(executor.url)
      .post(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    executor.execute(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPostJsonAsync[T](executor: RequestExecutor, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(executor.url)
      .post(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    executor.executeAsync(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPut[T](executor: RequestExecutor, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(executor.url).put(formBuilder.build())
    executor.execute(httpClient, builder, configurer, c.runtimeClass)

  }

  def httpPutAsync[T](executor: RequestExecutor, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val formBuilder = new FormBody.Builder()
    params.foreach { case (key, value) => formBuilder.add(key, value) }

    val builder = new Request.Builder().url(executor.url).put(formBuilder.build())

    executor.executeAsync(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPutJson[T](executor: RequestExecutor, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(executor.url)
      .put(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    executor.execute(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpPutJsonAsync[T](executor: RequestExecutor, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(executor.url)
      .put(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))

    executor.executeAsync(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpDelete[T](executor: RequestExecutor, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    val builder = new Request.Builder().url(executor.url).delete()

    executor.execute(httpClient, builder, configurer, c.runtimeClass)
  }

  def httpDeleteAsync[T](executor: RequestExecutor, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    val builder = new Request.Builder().url(executor.url).delete()

    executor.executeAsync(httpClient, builder, configurer, c.runtimeClass)
  }

}



