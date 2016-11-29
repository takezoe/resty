package com.github.takezoe.resty.model

import java.lang.reflect.Method

import com.github.takezoe.resty.util.ReflectionUtils

sealed trait ParamDef {
  val name: String
  val converter: ParamConverter
}

object ParamDef {

  def apply(name: String, method: Method, index: Int, clazz: Class[_]): ParamDef = {
    simpleTypeConverter(name, clazz)
      .map { converter => Param(name, converter) }
      .getOrElse {
        if(clazz == classOf[Seq[_]]){
          Param(name, new ParamConverter.SeqStringConverter(name, getWrappedTypeConverter(name, method, index)))
        } else if(clazz == classOf[Option[_]]){
          Param(name, new ParamConverter.OptionStringConverter(name, getWrappedTypeConverter(name, method, index)))
        } else {
          Body(name, clazz, new ParamConverter.JsonConverter(name, clazz))
        }
      }
  }

  protected def getWrappedTypeConverter(name: String, method: java.lang.reflect.Method, index: Int): ParamConverter = {
    ReflectionUtils.getWrappedTypeOfMethodArgument(method, index)
      .flatMap { t => simpleTypeConverter(name, t) }
      .getOrElse(new ParamConverter.StringConverter(name))
  }

  protected def simpleTypeConverter(name: String, clazz: Class[_]): Option[ParamConverter] = {
    if(clazz == classOf[String]) {
      Some(new ParamConverter.StringConverter(name))
    } else if(clazz == classOf[Int]){
      Some(new ParamConverter.IntConverter(name))
    } else if(clazz == classOf[Long]){
      Some(new ParamConverter.LongConverter(name))
    } else if(clazz == classOf[Boolean]){
      Some(new ParamConverter.BooleanConverter(name))
    } else {
      None
    }
  }

  // TODO Path, query, form or header
  case class Param(name: String, converter: ParamConverter) extends ParamDef

  case class Body(name: String, clazz: Class[_], converter: ParamConverter) extends ParamDef

}

