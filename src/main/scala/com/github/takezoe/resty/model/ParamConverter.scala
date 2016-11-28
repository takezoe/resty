package com.github.takezoe.resty.model

import com.github.takezoe.resty.util.JsonUtils

trait ParamConverter {
  def convert(values: Seq[String]): Either[String, AnyRef]
}

object ParamConverter {

  class StringConverter(name: String) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Left(s"${name} is required.")
      } else {
        Right(values.head)
      }
    }
  }

  class SeqStringConverter(name: String) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null) {
        Right(Seq.empty)
      } else {
        Right(values)
      }
    }
  }

  class OptionStringConverter(name: String) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Right(None)
      } else {
        Right(Some(values.head))
      }
    }
  }

  class JsonConverter(clazz: Class[_]) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Left("Body is required")
      } else {
        try {
          Right(JsonUtils.deserialize(values.head, clazz))
        } catch {
          case e: Exception => Left(e.getMessage)
        }
      }
    }
  }

}
