package com.github.takezoe.resty

import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

import com.github.takezoe.resty.model.{ActionDef, AppInfo, ControllerDef, ParamDef}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Manages all actions information of the application.
 */
object Resty {

  private val _appInfo = new AtomicReference[AppInfo](AppInfo())
  private val _actions = new CopyOnWriteArrayList[(ControllerDef, ActionDef)]()

  def register(appInfo: AppInfo): Unit = {
    _appInfo.set(appInfo)
  }

  def appInfo = _appInfo.get()

  def register(controller: AnyRef): Unit = {
    val annotation = controller.getClass.getAnnotation(classOf[Controller])

    val controllerDef = ControllerDef(
      name        = if(annotation != null) annotation.name() else "",
      description = if(annotation != null) annotation.description() else "",
      instance    = controller
    )

    val controllerClass = controller.getClass

    controllerClass.getMethods.foreach { method =>
      val annotation = method.getAnnotation(classOf[Action])
      if(annotation != null){
        _actions.add((controllerDef, ActionDef(
          annotation.method().toLowerCase(),
          annotation.path(),
          annotation.description(),
          annotation.deprecated(),
          getParamDefs(method, controllerClass, annotation),
          method,
          method.getReturnType == classOf[Future[_]]
        )))
      }
    }
  }

  protected def getParamDefs(actionMethod: Method, controllerClass: Class[_], action: Action): Seq[ParamDef] = {
    actionMethod.getParameters.zipWithIndex.map { case (param, i) =>
      actionMethod.getParameterAnnotations()(i).find(_.annotationType() == classOf[Param]).map { case x: Param =>
        // @Param is specified
        val paramName = if(x.name().nonEmpty) x.name else param.getName
        val paramType = param.getType
        ParamDef(
          from        = paramFrom(x.from(), action.path(), paramName, actionMethod, i, paramType),
          name        = paramName,
          description = x.description(),
          method      = actionMethod,
          index       = i,
          clazz       = paramType
        )
      }.getOrElse {
        // @Param is not specified
        val paramName = param.getName
        val paramType = param.getType
        ParamDef(
          from        = paramFrom("", action.path(), paramName, actionMethod, i, paramType),
          name        = paramName,
          description = "",
          method      = actionMethod,
          index       = i,
          clazz       = paramType
        )
      }
    }
  }

  protected def paramFrom(from: String, path: String, name: String,
                          actionMethod: Method, index: Int, clazz: Class[_]): String = {
    val f = if(from.nonEmpty) from else {
      if(ParamDef.isSimpleType(clazz) || ParamDef.isSimpleContainerType(actionMethod, index, clazz)){
        if(path.contains(s"{${name}}")) {
          "path"
        } else {
          "query"
        }
      } else {
        "body"
      }
    }
    println(clazz + ": " + f)
    f
  }

  def findAction(path: String, method: String): Option[(ControllerDef, ActionDef, Map[String, Seq[String]])] = {
    val pathParams = new mutable.HashMap[String, Seq[String]]()

    _actions.asScala.filter(_._2.method == method).find { case (controller, action) =>
      val requestPath = path.split("/")
      val actionPath = action.path.split("/")
      if(requestPath.length == actionPath.length){
        (requestPath zip actionPath).forall { case (requestPathFragment, actionPathFragment) =>
          if(actionPathFragment.startsWith("{") && actionPathFragment.endsWith("}")){
            pathParams += (actionPathFragment.substring(1, actionPathFragment.length - 1) -> Seq(requestPathFragment))
            true
          } else {
            requestPathFragment == actionPathFragment
          }
        }
      } else false
    }.map { case (controller, action) => (controller, action, pathParams.toMap) }
  }

  def allActions: Seq[(ControllerDef, ActionDef)] = _actions.asScala

}
