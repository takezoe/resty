package com.github.takezoe.resty.model

import com.github.takezoe.resty.util.JsonUtils
import io.swagger.models.RefModel
import io.swagger.models.parameters.{BodyParameter, Parameter, SerializableParameter}
import io.swagger.models.properties.{BooleanProperty, IntegerProperty, LongProperty, StringProperty}

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
      param
    }
  }

  class SeqStringConverter(name: String, converter: ParamConverter) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null) {
        Right(Seq.empty)
      } else {
        val converted: Seq[Either[String, AnyRef]] = values.map { x => converter.convert(Seq(x)) }
        converted.find(_.isLeft).getOrElse(Right(converted.map(_.right.get)))
      }
    }
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      param.setName(name)
      param.setType("array")
      converter match {
        case _: StringConverter  => param.setItems(new StringProperty())
        case _: IntConverter     => param.setItems(new IntegerProperty())
        case _: LongConverter    => param.setItems(new LongProperty())
        case _: BooleanConverter => param.setItems(new BooleanProperty())
      }
      param
    }
  }

  class OptionStringConverter(name: String, converter: ParamConverter) extends ParamConverter {
    override def convert(values: Seq[String]): Either[String, AnyRef] = {
      if (values == null || values.isEmpty) {
        Right(None)
      } else {
        converter.convert(values) match {
          case Right(x) => Right(Some(x))
          case Left(x)  => Left(x)
        }
      }
    }
    override def parameter(model: Parameter): Parameter = {
      val param = model.asInstanceOf[SerializableParameter]
      converter.parameter(param)
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
