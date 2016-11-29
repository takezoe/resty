package com.github.takezoe.resty.model

import com.github.takezoe.resty.util.JsonUtils
import io.swagger.models.RefModel
import io.swagger.models.parameters.{BodyParameter, Parameter, SerializableParameter}
import io.swagger.models.properties.{Property, StringProperty}

trait ParamConverter {
  def convert(values: Seq[String]): Either[String, AnyRef]
  def parameter(model: Parameter): Parameter
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
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("string")
      //param.setRequired(true)
      param
    }
  }

  class IntConverter(name: String) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Left(s"${name} is required.")
      } else {
        Right(new java.lang.Integer(values.head))
      }
    }
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("integer")
      param.setFormat("int32")
      //param.setRequired(true)
      param
    }
  }

  class LongConverter(name: String) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Left(s"${name} is required.")
      } else {
        Right(new java.lang.Long(values.head))
      }
    }
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("integer")
      param.setFormat("int64")
      //param.setRequired(true)
      param
    }
  }

  class BooleanConverter(name: String) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Left(s"${name} is required.")
      } else {
        Right(new java.lang.Boolean(values.head))
      }
    }
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("boolean")
      //param.setRequired(true)
      param
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
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("array")
      param.setItems(new StringProperty())
      param
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
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("string")
      param.setRequired(false)
      param
    }
  }

  class JsonConverter(name: String, clazz: Class[_]) extends ParamConverter {
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
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[BodyParameter]
      param.setSchema(new RefModel(clazz.getSimpleName))
      param.setName(name)
      param.setRequired(true)
      param
    }
  }

}
