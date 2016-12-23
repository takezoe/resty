package com.github.takezoe.resty

import java.io.{File, FileInputStream, InputStream}
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.ServletContextEvent
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.model.{ActionDef, ControllerDef, ParamDef}
import com.github.takezoe.resty.servlet.ConfigKeys
import com.github.takezoe.resty.util.{JsonUtils, StringUtils}
import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixCommandKey, HystrixCommandProperties}
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

trait RestyKernel {

  private val logger = LoggerFactory.getLogger(classOf[RestyKernel])

  protected def processAction(request: HttpServletRequest, response: HttpServletResponse, method: String): Unit = {
    Resty.findAction(request.getRequestURI, method) match {
      case Some((controller, action, pathParams)) => {
        try {
          if(HystrixSupport.isEnabled) {
            new HystrixSupport.RestyActionCommand(action.method + " " + action.path,
              invokeAction(controller, action, pathParams, request, response)
            ).execute()
          } else {
            invokeAction(controller, action, pathParams, request, response)
          }
        } catch {
          case e: HystrixRuntimeException =>
            val cause = e.getCause match {
              case e: InvocationTargetException => e.getCause
              case e => e
            }
            logger.error("Error during processing action", cause)
            processResponse(response, ActionResult(500, ErrorModel(Seq(cause.toString))))
          case e: InvocationTargetException =>
            logger.error("Error during processing action", e.getCause)
            processResponse(response, ActionResult(500, ErrorModel(Seq(e.getCause.toString))))
          case e: Exception =>
            logger.error("Error during processing action", e)
            processResponse(response, ActionResult(500, ErrorModel(Seq(e.toString))))
        }
      }
      case None => {
        processResponse(response, ActionResult(404, ()))
      }
    }
  }

  protected def invokeAction(controller: ControllerDef, action: ActionDef, pathParams: Map[String, Seq[String]],
                             request: HttpServletRequest, response: HttpServletResponse): Unit = {
    try {
      setServletAPI(controller, request, response)
      prepareParams(request, pathParams, action.params) match {
        case Left(errors) =>
          processResponse(response, BadRequest(ErrorModel(errors)))
        case Right(params) =>
          val result = action.function.invoke(controller.instance, params: _*)
          processResponse(response, result)
      }
    } catch {
      case e: InvocationTargetException => e.getCause match {
        case e: ActionResultException => processResponse(response, e.result)
        case e => throw e
      }
      case e: ActionResultException => processResponse(response, e.result)
    } finally {
      removeServletAPI(controller)
    }
  }

  protected def setServletAPI(controller: ControllerDef, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    controller.instance match {
      case x: ServletAPI => {
        x.requestHolder.set(request)
        x.responseHolder.set(response)
      }
      case _ => {}
    }
  }

  protected def removeServletAPI(controller: ControllerDef): Unit = {
    controller.instance match {
      case x: ServletAPI => {
        x.requestHolder.remove()
        x.responseHolder.remove()
      }
      case _ => {}
    }
  }

  protected def prepareParams(request: HttpServletRequest,
                              pathParams: Map[String, Seq[String]],
                              paramDefs: Seq[ParamDef]): Either[Seq[String], Seq[AnyRef]] = {
    val converted = paramDefs.map { paramDef =>
      paramDef match {
        case ParamDef.PathParam(name, _, converter) =>
          converter.convert(pathParams.get(name).getOrElse(request.getParameterValues(name)))
        case ParamDef.QueryParam(name, _, converter) =>
          converter.convert(pathParams.get(name).getOrElse(request.getParameterValues(name)))
        case ParamDef.HeaderParam(name, _, converter) =>
          converter.convert(Seq(request.getHeader(name)))
        case ParamDef.BodyParam(_, _, _, converter) =>
          converter.convert(Seq(IOUtils.toString(request.getInputStream, "UTF-8")))
      }
    }

    val errors = converted.collect { case Left(errorMessage) => errorMessage }

    if(errors.nonEmpty){
      Left(errors)
    } else {
      Right(converted.collect { case Right(value) => value })
    }
  }

  protected def processResponse(response: HttpServletResponse, result: Any): Unit = {
    result match {
      case x: Unit => {}
      case x: String => {
        if(response.getContentType == null) {
          response.setContentType("text/plain; charset=UTF-8")
        }
        val writer = response.getWriter
        writer.println(x)
        writer.flush()
      }
      case x: ActionResult[_] => {
        response.setStatus(x.status)
        x.headers.foreach { case (key, value) =>
          response.addHeader(key, value)
        }
        x.body match {
          case body: AnyRef => processResponse(response, body)
          case _ =>
        }
      }
      case x: Array[Byte] =>
        if(response.getContentType == null) {
          response.setContentType("application/octet-stream")
        }
        val out = response.getOutputStream
        out.write(x)
        out.flush()
      case x: InputStream =>
        if(response.getContentType == null) {
          response.setContentType("application/octet-stream")
        }
        try {
          val out = response.getOutputStream
          IOUtils.copy(x, out)
          out.flush()
        } finally {
          IOUtils.closeQuietly(x)
        }
      case x: File => {
        if(response.getContentType == null) {
          response.setContentType("application/octet-stream")
        }
        val in = new FileInputStream(x)
        try {
          val out = response.getOutputStream
          IOUtils.copy(in, out)
          out.flush()
        } finally {
          IOUtils.closeQuietly(in)
        }
      }
      case x: AnyRef => {
        if(response.getContentType == null) {
          response.setContentType("application/json")
        }
        val writer = response.getWriter
        writer.println(JsonUtils.serialize(x))
        writer.flush()
      }
    }
  }


}


object HystrixSupport {

  private val enable = new AtomicBoolean(false)

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

  def initialize(sce: ServletContextEvent): Unit = {
    if("enable" == StringUtils.trim(sce.getServletContext.getInitParameter(ConfigKeys.HystrixSupport))){
      enable.set(true)
    }
  }

  def shutdown(sce: ServletContextEvent): Unit = {
  }

  def isEnabled = enable.get()

}
