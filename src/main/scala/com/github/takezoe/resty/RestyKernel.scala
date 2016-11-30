package com.github.takezoe.resty

import java.io.{File, FileInputStream, InputStream}
import java.lang.reflect.InvocationTargetException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.model.{ControllerDef, ParamDef}
import com.github.takezoe.resty.util.JsonUtils
import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixCommandKey}
import org.apache.commons.io.IOUtils

trait RestyKernel {

  protected def processAction(request: HttpServletRequest, response: HttpServletResponse, method: String): Unit = {
    Resty.findAction(request.getRequestURI, method) match {
      case Some((controller, action, pathParams)) => {
        try {
          new RestyActionCommand(action.method + " " + action.path,
            try {
              setServletAPI(controller, request, response)
              prepareParams(request, pathParams, action.params) match {
                case Left(errors) =>
                  processResponse(response, BadRequest(ErrorModel(errors)))
                case Right(params) =>
                  val result = action.function.invoke(controller.instance, params: _*)
                  processResponse(response, result)
              }
            } finally {
              removeServletAPI(controller)
            }
          ).execute()
        } catch {
          case e: HystrixRuntimeException => {
            val cause = e.getCause match {
              case e: InvocationTargetException => e.getCause
              case e => e
            }
            processResponse(response, InternalServerError(ErrorModel(Seq(cause.toString))))
          }
        }
      }
      case None => {
        processResponse(response, NotFound())
      }
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

  protected def processResponse(response: HttpServletResponse, result: AnyRef): Unit = {
    result match {
      case x: String => {
        response.setContentType("text/plain; charset=UTF-8")
        val writer = response.getWriter
        writer.println(x)
        writer.flush()
      }
      case x: ActionResult => {
        response.setStatus(x.status)
        x.headers.foreach { case (key, value) =>
          response.addHeader(key, value)
        }
        x.body.foreach { body =>
          processResponse(response, body)
        }
      }
      case x: Array[Byte] =>
        response.setContentType("application/octet-stream")
        val out = response.getOutputStream
        out.write(x)
        out.flush()
      case x: InputStream =>
        response.setContentType("application/octet-stream")
        try {
          val out = response.getOutputStream
          IOUtils.copy(x, out)
          out.flush()
        } finally {
          IOUtils.closeQuietly(x)
        }
      case x: File => {
        response.setContentType("application/octet-stream")
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
        response.setContentType("application/json")
        val writer = response.getWriter
        writer.println(JsonUtils.serialize(x))
        writer.flush()
      }
    }
  }

  private class RestyActionCommand(key: String, f: => Unit) extends HystrixCommand[Unit](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RestyAction"))
      .andCommandKey(HystrixCommandKey.Factory.asKey(key))
  ) {
    override def run(): Unit = f
  }

}
