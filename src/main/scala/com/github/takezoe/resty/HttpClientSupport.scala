package com.github.takezoe.resty

import java.io.IOException
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
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

/**
 * Base trait for the HTTP request target.
 */
trait RequestTarget {

  def execute[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Either[ErrorModel, T]

  def executeAsync[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Future[Either[ErrorModel, T]]

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

/**
 * Implementation of [[RequestTarget]] for single url.
 */
class SimpleRequestTarget(val url: String, config: HttpClientConfig) extends RequestTarget {

  private val disabledTime = new AtomicReference[Option[Long]](None)
  private val failureCount = new AtomicInteger(0)

  def isAvailable: Boolean = {
    (config.maxFailure <= 0 || failureCount.get() <= config.maxFailure)
  }

  def execute[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Either[ErrorModel, T] = {
    try {
      checkWhetherEnabled()

      val builder = new Request.Builder()
      configurer(url, builder)

      val request = builder.build()
      val response = withRetry(httpClient, request, config)

      // reset failure counter
      disabledTime.set(None)
      failureCount.set(0)

      handleResponse(request, response, clazz)

    } catch {
      case e: Exception => {
        if(config.maxFailure > 0 && failureCount.incrementAndGet() > config.maxFailure){
          disabledTime.set(Some(System.currentTimeMillis))
        }
        Left(ErrorModel(Seq(e.toString)))
      }
    }
  }

  protected def checkWhetherEnabled(): Unit = {
    if(config.maxFailure <= 0 || failureCount.get() < config.maxFailure){
      ()
    } else {
      disabledTime.get() match {
        case None => ()
        case Some(time) if time <= System.currentTimeMillis - config.resetInterval => {
          failureCount.set(config.maxFailure - 1)
          disabledTime.set(None)
        }
        case Some(_) => throw new RuntimeException(s"${url} is not available now.")
      }
    }
  }

  protected def withRetry(httpClient: OkHttpClient, request: Request, config: HttpClientConfig): Response = {
    var count = 0
    while(true){
      try {
        return httpClient.newCall(request).execute()
      } catch {
        case _: Exception if count < config.maxRetry => {
          count = count + 1
          Thread.sleep(config.retryInterval)
        }
      }
    }
    ???
  }

  def executeAsync[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Future[Either[ErrorModel, T]] = {
    try {
      checkWhetherEnabled()

      val builder = new Request.Builder()
      configurer(url, builder)

      val request = builder.build()
      val promise = Promise[Either[ErrorModel, T]]()

      httpClient.newCall(request).enqueue(new Callback {
        var retryCount = 0
        override def onFailure(call: Call, e: IOException): Unit = {
          if(retryCount < config.maxRetry){
            retryCount = retryCount + 1
            Thread.sleep(config.retryInterval) // TODO Don't brock a thread here!
            httpClient.newCall(request)
          } else {
            if(config.maxFailure > 0 && failureCount.incrementAndGet() > config.maxFailure){
              disabledTime.set(Some(System.currentTimeMillis))
            }
            promise.failure(e)
          }
        }
        override def onResponse(call: Call, response: Response): Unit = {
          // reset failure counter
          disabledTime.set(None)
          failureCount.set(0)

          promise.success(handleResponse(request, response, clazz))
        }
      })

      promise.future

    } catch {
      case e: Exception => Future.successful(Left(ErrorModel(Seq(e.toString))))
    }
  }
}

/**
 * Implementation of [[RequestTarget]] for multiple urls. This implementation chooses a url from given urls randomly.
 */
class RandomRequestTarget(val urls: Seq[String], config: HttpClientConfig) extends RequestTarget {

  private val targets = urls.map(url => new SimpleRequestTarget(url, config))

  protected def nextTarget: Option[RequestTarget] = {
    val availableTargets = targets.filter((_.isAvailable))
    if(availableTargets.isEmpty){
      None
    } else {
      Some(availableTargets((scala.math.random * availableTargets.length).toInt))
    }
  }

  override def execute[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Either[ErrorModel, T] = {
    nextTarget match {
      case Some(target) => target.execute(httpClient, configurer, clazz)
      case None => Left(ErrorModel(Seq("No available url!")))
    }
  }

  override def executeAsync[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Future[Either[ErrorModel, T]] = {
    nextTarget match {
      case Some(target) => target.executeAsync(httpClient, configurer, clazz)
      case None => Future.successful(Left(ErrorModel(Seq("No available url!"))))
    }
  }

}

/**
 * Configuration of behavior of HttpClient.
 *
 * @param maxRetry default is 0 means no retry
 * @param retryInterval msec. default is 0 means retry immediately
 * @param maxFailure default is 0 means disabling circuit breaker
 * @param resetInterval msec. default is 60000
 */
case class HttpClientConfig(maxRetry: Int = 0, retryInterval: Int = 0, maxFailure: Int = 0, resetInterval: Int = 60000)

/**
 * HTTP client with Zipkin support.
 */
trait HttpClientSupport {

  implicit def httpClientConfig: HttpClientConfig = HttpClientConfig()
  implicit def string2target(url: String)(implicit config: HttpClientConfig): SimpleRequestTarget = new SimpleRequestTarget(url, config)
  implicit def stringSeq2target(urls: Seq[String])(implicit config: HttpClientConfig): RandomRequestTarget = new RandomRequestTarget(urls, config)

  protected def httpClient = HttpClientSupport.httpClient

  def httpGet[T](target: RequestTarget, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    target.execute(httpClient, (url, builder) => {
      builder.url(url).get()
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpGetAsync[T](target: RequestTarget, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    target.executeAsync(httpClient, (url, builder) => {
      builder.url(url).get()
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPost[T](target: RequestTarget, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    target.execute(httpClient, (url, builder) => {
      val formBuilder = new FormBody.Builder()
      params.foreach { case (key, value) => formBuilder.add(key, value) }

      builder.url(url).post(formBuilder.build())

      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPostAsync[T](target: RequestTarget, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    target.executeAsync(httpClient, (url, builder) => {
      val formBuilder = new FormBody.Builder()
      params.foreach { case (key, value) => formBuilder.add(key, value) }

      builder.url(url).post(formBuilder.build())

      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPostJson[T](target: RequestTarget, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    target.execute(httpClient, (url, builder) => {
      builder.url(url).post(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPostJsonAsync[T](target: RequestTarget, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    target.executeAsync(httpClient, (url, builder) => {
      builder.url(url).post(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPut[T](target: RequestTarget, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    target.execute(httpClient, (url, builder) => {
      val formBuilder = new FormBody.Builder()
      params.foreach { case (key, value) => formBuilder.add(key, value) }

      builder.url(url).put(formBuilder.build())

      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPutAsync[T](target: RequestTarget, params: Map[String, String], configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    target.executeAsync(httpClient, (url, builder) => {
      val formBuilder = new FormBody.Builder()
      params.foreach { case (key, value) => formBuilder.add(key, value) }

      builder.url(url).put(formBuilder.build())

      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPutJson[T](target: RequestTarget, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    target.execute(httpClient, (url, builder) => {
      builder.url(url).put(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpPutJsonAsync[T](target: RequestTarget, doc: AnyRef, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    target.executeAsync(httpClient, (url, builder) => {
      builder.url(url).put(RequestBody.create(HttpClientSupport.ContentType_JSON, JsonUtils.serialize(doc)))
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpDelete[T](target: RequestTarget, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Either[ErrorModel, T] = {
    target.execute(httpClient, (url, builder) => {
      builder.url(url).delete()
      configurer(builder)
    }, c.runtimeClass)
  }

  def httpDeleteAsync[T](target: RequestTarget, configurer: Request.Builder => Unit = (builder) => ())(implicit c: ClassTag[T]): Future[Either[ErrorModel, T]] = {
    target.executeAsync(httpClient, (url, builder) => {
      builder.url(url).delete()
      configurer(builder)
    }, c.runtimeClass)
  }

}



