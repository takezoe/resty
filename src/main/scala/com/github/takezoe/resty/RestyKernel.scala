package com.github.takezoe.resty

import java.io.{File, FileInputStream, InputStream}
import java.lang.reflect.InvocationTargetException
import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.model.{ActionDef, ControllerDef, ParamDef}
import com.github.takezoe.resty.util.JsonUtils
import com.netflix.hystrix.exception.HystrixRuntimeException
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}
import scala.concurrent.Future

trait RestyKernel {

  private val logger = LoggerFactory.getLogger(classOf[RestyKernel])
  private val injector = new ParamInjector()

  protected def processAction(request: HttpServletRequest, response: HttpServletResponse, method: String): Unit = {
    injector.withValues(request, response){
      val path = request.getRequestURI.substring(request.getContextPath.length)

      Resty.findAction(path, method) match {
        case Some((controller, action, pathParams)) => {
          if(action.async){
            processAsyncAction(request, response, method, controller, action, pathParams)
          } else {
            processSyncAction(request, response, method, controller, action, pathParams)
          }
        }
        case None => {
          processResponse(response, ActionResult(404, ()))
        }
      }
    }
  }

  protected def processSyncAction(request: HttpServletRequest, response: HttpServletResponse, method: String,
      controller: ControllerDef, action: ActionDef, pathParams: Map[String, Seq[String]]): Unit = {
    try {
      val result = if(HystrixSupport.isEnabled) {
        new HystrixSupport.RestyActionCommand(
          action.method + " " + action.path,
          invokeAction(controller, action, pathParams, request, response)
        ).execute()
      } else {
        invokeAction(controller, action, pathParams, request, response)
      }

      processResponse(response, result)

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

  protected def processAsyncAction(request: HttpServletRequest, response: HttpServletResponse, method: String,
      controller: ControllerDef, action: ActionDef, pathParams: Map[String, Seq[String]]): Unit = {
    val asyncContext = request.startAsync(request, response)
    val future = invokeAsyncAction(controller, action, pathParams, request, response, asyncContext)
    if(HystrixSupport.isEnabled){
      new HystrixSupport.RestyAsyncActionCommand(action.method + " " + action.path, future, Resty.ioExecutionContext)
        .toObservable.subscribe(
        (result: AnyRef) => {
          processResponse(response, result)
          asyncContext.complete()
        },
        (error: Throwable) => {
          val cause = error match {
            case e: HystrixRuntimeException => e.getCause
            case e => e
          }
          logger.error("Error during processing action", cause)
          processResponse(response, ActionResult(500, ErrorModel(Seq(cause.toString))))
          asyncContext.complete()
        }
      )
    } else {
      future.onComplete {
        case Success(result) => {
          processResponse(response, result)
          asyncContext.complete()
        }
        case Failure(error) => {
          processResponse(response, ActionResult(500, ErrorModel(Seq(error.toString))))
          asyncContext.complete()
        }
      }(Resty.ioExecutionContext)
    }
  }

  protected def invokeAsyncAction(controller: ControllerDef, action: ActionDef, pathParams: Map[String, Seq[String]],
      request: HttpServletRequest, response: HttpServletResponse, context: AsyncContext): Future[AnyRef] = {
    try {
      prepareParams(request, pathParams, action.params) match {
        case Left(errors)  => Future.successful(BadRequest(ErrorModel(errors)))
        case Right(params) => action.function.invoke(controller.instance, params: _*).asInstanceOf[Future[AnyRef]]
      }
    } catch {
      case e: InvocationTargetException => e.getCause match {
        case e: ActionResultException => Future.successful(e.result)
        case e => Future.failed(e)
      }
      case e: ActionResultException =>  Future.successful(e.result)
    }
  }

  protected def invokeAction(controller: ControllerDef, action: ActionDef, pathParams: Map[String, Seq[String]],
      request: HttpServletRequest, response: HttpServletResponse): AnyRef = {
    prepareParams(request, pathParams, action.params) match {
      case Left(errors) => BadRequest(ErrorModel(errors))
      case Right(params) => action.function.invoke(controller.instance, params: _*)
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
        case ParamDef.InjectParam(_, _, clazz, _) =>
          Right(injector.get(clazz))
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
      case null => {}
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
          response.setHeader("Content-Disposition", "attachment; filename=\"" + x.getName + "\"")
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
        val out = response.getOutputStream
        out.write(JsonUtils.serialize(x).getBytes("UTF-8"))
        out.flush()
      }
    }
  }

}

