package com.github.takezoe.resty.util

import org.json4s.scalap.scalasig._

object ReflectionUtils {

  def getWrappedTypeOfMethod[T](method: java.lang.reflect.Method)(implicit m: Manifest[T]): Option[Class[_]] = {

    def findArgType(c: ClassSymbol, s: MethodSymbol, typeArgIdx: Int): Class[_] = {
      println(s.infoType)
      val t = s.infoType match {
        case MethodType(TypeRefType(_, _, args), _) => args(0)
      }

      toClass(t match {
        case TypeRefType(_, symbol, _)   => symbol
        case x => throw new Exception("Unexpected type info " + x)
      })
    }

    val scalaSigOption = ScalaSigParser.parse(method.getDeclaringClass())

    scalaSigOption flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses
      val _type = syms.collectFirst {
        case c if (c.path == method.getDeclaringClass().getName) =>
          findMethodSymbol(c, method.getName).map { f => findArgType(c, f, 0) }
      }
      _type.flatten
    }
  }


  def getWrappedTypeOfMethodArgument[T](method: java.lang.reflect.Method, index: Int)(implicit m: Manifest[T]): Option[Class[_]] = {

    def findArgType(c: ClassSymbol, s: MethodSymbol, typeArgIdx: Int): Class[_] = {
      val t = s.infoType match {
        case MethodType(TypeRefType(_, _, args), paramSymbols) => {
          paramSymbols(typeArgIdx) match {
            case sym: MethodSymbol => sym.infoType match {
              case TypeRefType(_, _, args) => args(0)
            }
          }
        }
      }

      toClass(t match {
        case TypeRefType(_, symbol, _)   => symbol
        case x => throw new Exception("Unexpected type info " + x)
      })
    }

    val scalaSigOption = ScalaSigParser.parse(method.getDeclaringClass())

    scalaSigOption flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses
      val _type = syms.collectFirst {
        case c if (c.path == method.getDeclaringClass().getName) =>
          findMethodSymbol(c, method.getName).map { f => findArgType(c, f, index) }
      }
      _type.flatten
    }
  }

  def getWrappedTypeOfField[T](field: java.lang.reflect.Field)(implicit m: Manifest[T]): Option[Class[_]] = {

    def findArgType(c: ClassSymbol, s: MethodSymbol, typeArgIdx: Int): Class[_] = {
      val t = s.infoType match {
        case NullaryMethodType(TypeRefType(_, _, args)) => args(typeArgIdx)
      }

      toClass(t match {
        case TypeRefType(_, symbol, _)   => symbol
        case x => throw new Exception("Unexpected type info " + x)
      })
    }

    val scalaSigOption = ScalaSigParser.parse(field.getDeclaringClass())

    scalaSigOption flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses
      val _type = syms.collectFirst {
        case c if (c.path == field.getDeclaringClass().getName) =>
          findMethodSymbol(c, field.getName).map { f => findArgType(c, f, 0) }
      }
      _type.flatten
    }
  }

  def findMethodSymbol(c: ClassSymbol, name: String): Option[MethodSymbol] =
    (c.children collect { case m: MethodSymbol if m.name == name => m }).headOption

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

}
