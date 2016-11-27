package com.github.takezoe.resty.model

import com.github.takezoe.resty.util.JsonUtils

trait ParamConverter {
  def convert(value: String): Either[String, AnyRef]
}

object ParamConverter {

  class StringConverter(name: String) extends ParamConverter {
    override def convert(value: String): Either[String, AnyRef] = {
      if (value == null) {
        Left(s"${name} is required.")
      } else {
        Right(value)
      }
    }
  }

  class OptionStringConverter(name: String) extends ParamConverter {
    override def convert(value: String): Either[String, AnyRef] = {
      if (value == null) {
        Right(None)
      } else {
        Right(Some(value))
      }
    }
  }

  class JsonConverter(clazz: Class[_]) extends ParamConverter {
    override def convert(value: String): Either[String, AnyRef] = {
      if (value == null) {
        Left("Body is required")
      } else {
        try {
          Right(JsonUtils.deserialize(value, clazz))
        } catch {
          case e: Exception => Left(e.getMessage)
        }
      }
    }
  }

}
