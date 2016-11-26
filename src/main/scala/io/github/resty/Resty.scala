package io.github.resty

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

import io.github.resty.model.{AppInfo, ActionDef, ParamDef}

import scala.collection.mutable
import scala.collection.JavaConverters._

object Resty {

  private val _appInfo = new AtomicReference[AppInfo](AppInfo())
  private val _actions = new CopyOnWriteArrayList[ActionDef]()
  register(new SwaggerController())

  def register(appInfo: AppInfo): Unit = {
    _appInfo.set(appInfo)
  }

  def appInfo = _appInfo.get()

  def register(controller: AnyRef): Unit = {
    controller.getClass.getMethods.foreach { method =>
      val annotation = method.getAnnotation(classOf[io.github.resty.Action])
      if(annotation != null){
        val paramDefs = method.getParameters.map { param =>
          ParamDef(param.getName, param.getType)
        }
        _actions.add(ActionDef(
          annotation.method().toLowerCase(),
          annotation.path(),
          annotation.description(),
          annotation.deprecated(),
          paramDefs,
          method,
          controller
        ))
      }
    }
  }

  def findAction(path: String, method: String): Option[(ActionDef, Map[String, String])] = {
    val pathParams = new mutable.HashMap[String, String]()

    _actions.asScala.filter(_.method == method).find { action =>
      val requestPath = path.split("/")
      val actionPath = action.path.split("/")
      if(requestPath.length == actionPath.length){
        (requestPath zip actionPath).forall { case (requestPathFragment, actionPathFragment) =>
          if(actionPathFragment.startsWith("{") && actionPathFragment.endsWith("}")){
            pathParams += (actionPathFragment.substring(1, actionPathFragment.length - 1) -> requestPathFragment)
            true
          } else {
            requestPathFragment == actionPathFragment
          }
        }
      } else false
    }.map { action => (action, pathParams.toMap) }
  }

  def allActions: Seq[ActionDef] = _actions.asScala

}
