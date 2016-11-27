package io.github.takezoe.resty

import java.lang.reflect.InvocationTargetException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.exception.HystrixRuntimeException
import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixCommandKey}
import io.github.takezoe.resty.model.{ControllerDef, ParamDef}
import io.github.takezoe.resty.util.JsonUtils
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

  protected def prepareParams(request: HttpServletRequest, pathParams: Map[String, String], paramDefs: Seq[ParamDef]): Either[Seq[String], Seq[AnyRef]] = {
    val converted = paramDefs.map { paramDef =>
      paramDef match {
        case ParamDef.Param(name, converter) => converter.convert(pathParams.get(name).getOrElse(request.getParameter(name)))
        case ParamDef.Body(_, _, converter) => converter.convert(IOUtils.toString(request.getInputStream, "UTF-8"))
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
        x.body.foreach { body =>
          processResponse(response, body)
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
