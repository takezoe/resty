package io.github.takezoe.resty.util

import java.lang.reflect.Field

import org.json4s.scalap.scalasig._

object ReflectionUtils {

  def getWrappedType[T](field: Field)(implicit m: Manifest[T]): Option[Class[_]] = {

    def findField(c: ClassSymbol, name: String): Option[MethodSymbol] =
      (c.children collect { case m: MethodSymbol if m.name == name => m }).headOption

    def findArgTypeForField(s: MethodSymbol, typeArgIdx: Int): Class[_] = {
      val t = s.infoType match {
        case NullaryMethodType(TypeRefType(_, _, args)) => args(typeArgIdx)
      }

      toClass(t match {
        case TypeRefType(_, symbol, _)   => symbol
        case x => throw new Exception("Unexpected type info " + x)
      })
    }

    def toClass(s: Symbol) = s.path match {
      case "scala.Short"         => classOf[Short]
      case "scala.Int"           => classOf[Int]
      case "scala.Long"          => classOf[Long]
      case "scala.Boolean"       => classOf[Boolean]
      case "scala.Float"         => classOf[Float]
      case "scala.Double"        => classOf[Double]
      case "scala.Byte"          => classOf[Byte]
      case "scala.Predef.String" => classOf[String]
      case x                     => Class.forName(x)
    }

    val scalaSigOption = ScalaSigParser.parse(field.getDeclaringClass())

    scalaSigOption flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses
      val _type = syms.collectFirst {
        case c if (c.path == field.getDeclaringClass().getName) =>
          field match {
            case _: Field => findField(c, field.getName).map { f =>
              findArgTypeForField(f, 0)
            }
          }
      }
      _type.flatten
    }
  }

}
