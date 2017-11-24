package com.github.takezoe.resty

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.ServletContextEvent

import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.StringUtils
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
  class RestyActionCommand(key: String, f: => AnyRef) extends HystrixCommand[AnyRef](
    HystrixCommand.Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RestyAction"))
      .andCommandKey(HystrixCommandKey.Factory.asKey(key))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties.Setter()
          .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)
          .withExecutionIsolationSemaphoreMaxConcurrentRequests(1000))
  ) {
    override def run(): AnyRef = f
  }

  /**
   * HistrixCommand implementation for a asynchronous action.
   */
  class RestyAsyncActionCommand(key: String, future: Future[AnyRef], ec: ExecutionContext) extends HystrixObservableCommand[AnyRef](
    HystrixObservableCommand.Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RestyAction"))
      .andCommandKey(HystrixCommandKey.Factory.asKey(key))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties.Setter()
          .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)
          .withExecutionIsolationSemaphoreMaxConcurrentRequests(1000))
  ) {

    override def construct(): Observable[AnyRef] = {
      val channel = ReplaySubject.create[AnyRef]()

      future.onComplete {
        case Success(result) => {
          channel.onNext(result)
          channel.onCompleted()
        }
        case Failure(error) => {
          channel.onError(error)
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