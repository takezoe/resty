package com.github.takezoe.resty.model

import java.lang.reflect.Method

import com.github.takezoe.resty.util.ReflectionUtils

sealed trait ParamDef {
  val name: String
  val description: String
  val converter: ParamConverter
}

object ParamDef {

  def apply(from: String, name: String, description: String, method: Method, index: Int, clazz: Class[_]): ParamDef = {
    val converter = simpleTypeConverter(name, clazz)
      .getOrElse {
        if(clazz == classOf[Seq[_]] && isSimpleContainerType(method, index, clazz)) {
          new ParamConverter.SimpleSeqConverter(name, getWrappedTypeConverter(name, method, index))
        } else if(clazz.isArray && isSimpleContainerType(method, index, clazz)) {
          if(clazz.getComponentType == classOf[Byte]){
            new ParamConverter.ByteArrayConverter(name)
          } else {
            new ParamConverter.SimpleArrayConverter(name,
              simpleTypeConverter(name, clazz.getComponentType).getOrElse(new ParamConverter.StringConverter(name))
            )
          }
        } else if(clazz == classOf[Option[_]]){
          new ParamConverter.OptionConverter(name, getWrappedTypeConverter(name, method, index))
        } else {
          new ParamConverter.JsonConverter(name, clazz)
        }
      }

    from.toLowerCase() match {
      case "query"  => QueryParam(name, description, converter)
      case "path"   => PathParam(name, description, converter)
      case "header" => HeaderParam(name, description, converter)
      case "body"   => BodyParam(name, description, clazz, converter)
    }
  }

  def isSimpleType(clazz: Class[_]): Boolean = {
    clazz == classOf[String] || clazz == classOf[Int] || clazz == classOf[Long] || clazz == classOf[Boolean] || clazz == classOf[Byte]
  }

  def isSimpleContainerType(method: Method, index: Int, clazz: Class[_]): Boolean = {
    if(clazz == classOf[Option[_]]){
      true
    } else if(clazz == classOf[Seq[_]]) {
      val t = ReflectionUtils.getWrappedTypeOfMethodArgument(method, index)
      t.exists(isSimpleType)
    } else if(clazz.isArray){
      isSimpleType(clazz.getComponentType)
    } else {
      false
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

  case class PathParam(name: String, description: String, converter: ParamConverter) extends ParamDef
  case class QueryParam(name: String, description: String, converter: ParamConverter) extends ParamDef
  case class HeaderParam(name: String, description: String, converter: ParamConverter) extends ParamDef
  case class BodyParam(name: String, description: String, clazz: Class[_], converter: ParamConverter) extends ParamDef

}

