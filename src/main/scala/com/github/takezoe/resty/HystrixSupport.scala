package com.github.takezoe.resty

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.ServletContextEvent

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.StringUtils
import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy
import com.netflix.hystrix._
import rx.Observable
import rx.subjects.ReplaySubject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object HystrixSupport {

  private val enable = new AtomicBoolean(false)

  /**
   * HistrixCommand implementation for a synchronous action.
   */
  class RestyActionCommand(key: String, f: => Unit) extends HystrixCommand[Unit](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RestyAction"))
      .andCommandKey(HystrixCommandKey.Factory.asKey(key))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties.Setter()
          .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)
          .withExecutionIsolationSemaphoreMaxConcurrentRequests(1000))
  ) {
    override def run(): Unit = f
  }

  /**
   * HistrixCommand implementation for a asynchronous action.
   */
  class RestyAsyncActionCommand(key: String, future: Future[_])(implicit ec: ExecutionContext)
    extends HystrixObservableCommand[Any](HystrixCommandGroupKey.Factory.asKey(key)) {

    override def construct(): Observable[Any] = {
      val channel = ReplaySubject.create[Any]()

      future.onComplete {
        case Success(v) => {
          println("***** suceess ****")
          channel.onNext(v)
          channel.onCompleted()
        }
        case Failure(t) => {
          println("***** failure ****")
          channel.onError(t)
          channel.onCompleted()
        }
      }(ec)

      channel.asObservable()
    }
  }


  def initialize(sce: ServletContextEvent): Unit = {
    if("enable" == StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.HystrixSupport))){
      enable.set(true)
    }
  }

  def shutdown(sce: ServletContextEvent): Unit = {
  }

  def isEnabled = enable.get()

}