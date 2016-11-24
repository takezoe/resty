package io.github.resty.model

sealed trait ParamDef {
  val converter: ParamConverter
}

object ParamDef {

  def apply(name: String, clazz: Class[_]): ParamDef = {
    if(clazz == classOf[String]){
      Param(name, new ParamConverter.StringConverter(name))
    } else if(clazz == classOf[Option[_]]){
      Param(name, new ParamConverter.OptionStringConverter(name))
    } else {
      Body(clazz, new ParamConverter.JsonConverter(clazz))
    }
  }

  // TODO Path, query, form or header
  case class Param(name: String, converter: ParamConverter) extends ParamDef

  case class Body(clazz: Class[_], converter: ParamConverter) extends ParamDef

}

