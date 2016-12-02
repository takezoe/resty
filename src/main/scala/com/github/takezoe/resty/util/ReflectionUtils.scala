package com.github.takezoe.resty.util

import java.lang.reflect.Member

import org.json4s.scalap.scalasig._

import scala.annotation.tailrec

object ReflectionUtils {

  def getWrappedTypeOfMethod[T](method: java.lang.reflect.Method)(implicit m: Manifest[T]): Option[Class[_]] = {

    def findArgType(c: ClassSymbol, s: MethodSymbol, typeArgIdx: Int): Class[_] = {
      val t = s.infoType match {
        case MethodType(TypeRefType(_, _, args), _) => args(0)
      }
      toClass(t match {
        case TypeRefType(_, symbol, _)   => symbol
        case x => throw new Exception("Unexpected type info " + x)
      })
    }

    ScalaSigParser.parse(getTopLevelClass(method)).flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses.flatMap(getAllClassSymbols)
      val _type = syms.collectFirst {
        case c if (c.path == method.getDeclaringClass.getName.replace('$', '.')) =>
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

    ScalaSigParser.parse(getTopLevelClass(method)).flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses.flatMap(getAllClassSymbols)
      val _type = syms.collectFirst {
        case c if (c.path == method.getDeclaringClass.getName.replace('$', '.')) =>
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

    ScalaSigParser.parse(getTopLevelClass(field)).flatMap { scalaSig =>
      val syms = scalaSig.topLevelClasses.flatMap(getAllClassSymbols)
      val _type = syms.collectFirst {
        case c if (c.path == field.getDeclaringClass.getName.replace('$', '.')) =>
          findMethodSymbol(c, field.getName).map { f => findArgType(c, f, 0) }
      }
      _type.flatten
    }
  }

  protected def getAllClassSymbols(sym: ClassSymbol): Seq[ClassSymbol] = {
    sym.children.collect { case c: ClassSymbol =>
      getAllClassSymbols(c)
    }.flatten :+ sym
  }

  protected def getTopLevelClass(member: Member): Class[_] = {
    member.getDeclaringClass match {
      case c if c.getName.contains("$") => getTopLevelClass(c)
      case c => c
    }
  }

  @tailrec
  protected def getTopLevelClass(clazz: Class[_]): Class[_] = {
    clazz.getDeclaringClass match {
      case c if c.getName.contains("$") => getTopLevelClass(c)
      case c => c
    }
  }

  protected def findMethodSymbol(c: ClassSymbol, name: String): Option[MethodSymbol] =
    (c.children collect { case m: MethodSymbol if m.name == name => m }).headOption

  protected def toClass(s: Symbol) = s.path match {
    case "scala.Short"         => classOf[Short]
    case "scala.Int"           => classOf[Int]
    case "scala.Long"          => classOf[Long]
    case "scala.Boolean"       => classOf[Boolean]
    case "scala.Float"         => classOf[Float]
    case "scala.Double"        => classOf[Double]
    case "scala.Byte"          => classOf[Byte]
    case "scala.Predef.String" => classOf[String]
    case x => Class.forName(if(s.parent.exists(_.isInstanceOf[ClassSymbol])) x.replaceFirst("\\.([^.]+?)$", "\\$$1") else x)
  }

}
