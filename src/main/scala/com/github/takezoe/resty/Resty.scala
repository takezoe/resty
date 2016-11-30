package com.github.takezoe.resty

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

import com.github.takezoe.resty.model.{ActionDef, AppInfo, ControllerDef, ParamDef}

import scala.collection.mutable
import scala.collection.JavaConverters._

object Resty {

  private val _appInfo = new AtomicReference[AppInfo](AppInfo())
  private val _actions = new CopyOnWriteArrayList[(ControllerDef, ActionDef)]()
  register(new SwaggerController())

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

    controller.getClass.getMethods.foreach { method =>
      val annotation = method.getAnnotation(classOf[Action])
      if(annotation != null){
        val paramDefs = method.getParameters.zipWithIndex.map { case (param, i) =>
          method.getParameterAnnotations()(i).find(_.annotationType() == classOf[Param]).map { case x: Param =>
            // @Param is specified
            val paramName = if(x.name().nonEmpty) x.name else param.getName
            val paramType = param.getType
            ParamDef(paramFrom(x.from(), annotation.path(), paramName, paramType), paramName, x.description(), method, i, paramType)
          }.getOrElse {
            // @Param is not specified
            val paramName = param.getName
            val paramType = param.getType
            ParamDef(paramFrom("", annotation.path(), paramName, paramType), paramName, "", method, i, paramType)
          }
        }
        _actions.add((controllerDef, ActionDef(
          annotation.method().toLowerCase(),
          annotation.path(),
          annotation.description(),
          annotation.deprecated(),
          paramDefs,
          method
        )))
      }
    }
  }

  protected def paramFrom(from: String, path: String, name: String, clazz: Class[_]): String = {
    if(from.nonEmpty) from else {
      if(ParamDef.isSimpleType(clazz) || ParamDef.isContainerType(clazz)){
        if(path.contains(s"{${name}}")) {
          "path"
        } else {
          "query"
        }
      } else {
        "body"
      }
    }
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
