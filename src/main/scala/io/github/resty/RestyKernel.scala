package io.github.resty

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import io.github.resty.model.{ActionDef, ParamDef}
import io.github.resty.util.JsonUtils
import org.apache.commons.io.IOUtils

trait RestyKernel {

  protected def processAction(request: HttpServletRequest, response: HttpServletResponse, method: String): Unit = {
    Resty.findAction(request.getRequestURI, method) match {
      case Some((action, pathParams)) => {
        try {
          setServletAPI(action, request, response)
          prepareParams(request, pathParams, action.params) match {
            case Left(errors) =>
              processResponse(response, BadRequest(ErrorModel(errors)))
            case Right(params) =>
              val result = action.function.invoke(action.controller, params: _*)
              processResponse(response, result)
          }
        } catch {
          case e: Exception =>
            processResponse(response, InternalServerError(ErrorModel(Seq(e.toString))))
        }
      }
      case None => {
        processResponse(response, NotFound())
      }
    }
  }

  protected def setServletAPI(action: ActionDef, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    action.controller match {
      case x: ServletAPI => {
        x.requestHolder.set(request)
        x.responseHolder.set(response)
      }
      case _ => {}
    }
  }

  protected def removeServletAPI(action: ActionDef, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    action.controller match {
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
        case ParamDef.Body(_, converter) => converter.convert(IOUtils.toString(request.getInputStream, "UTF-8"))
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

}
