package com.github.takezoe.resty.util

import java.lang.reflect.Method

import scala.collection.mutable.ListBuffer

object ScaladocUtils {

  def getScaladoc(clazz: Class[_]): Option[String] = {
    val scaladoc = clazz.getAnnotation(classOf[com.github.takezoe.scaladoc.Scaladoc])
    if(scaladoc != null){
      Some(scaladoc.value())
    } else {
      None
    }
  }

  def getScaladoc(method: Method): Option[String] = {
    val scaladoc = method.getAnnotation(classOf[com.github.takezoe.scaladoc.Scaladoc])
    if(scaladoc != null){
      Some(scaladoc.value())
    } else {
      None
    }
  }

  def parseScaladoc(scaladoc: String): Scaladoc = {
    val sb = new StringBuilder()
    val tags = new ListBuffer[Tag]()
    var tag: Tag = null

    scaladoc.trim().replaceFirst("^/\\*+", "").replaceFirst("\\*+/$", "").split("\n").foreach { line =>
      val s = line.trim().replaceFirst("^\\*\\s*", "")
      if(s.startsWith("@")){
        if(tag != null){
          tags += tag
        }
        s.split("\\s", 2) match {
          case Array(tagName, description) =>
            tag = Tag(tagName, Some(description))
          case Array(tagName) =>
            tag = Tag(tagName, None)
        }
      } else if(tag != null){
        tag = tag.copy(description = Some(tag.description.getOrElse("") + "\n" + s))
      } else {
        sb.append(s + "\n")
      }
    }
    if(tag != null){
      tags += tag
    }

    Scaladoc(sb.toString().trim(), tags)
  }

  case class Scaladoc(description: String, tags: Seq[Tag])
  case class Tag(name: String, description: Option[String]){
    lazy val paramName: Option[String] = {
      description.flatMap { case s =>
        s.split("\\s", 2) match {
          case Array(paramName, paramDescription) =>
            Some(paramName)
          case Array(paramName) =>
            Some(paramName)
        }
      }
    }
    lazy val paramDescription: Option[String] = {
      description.flatMap { case s =>
        s.split("\\s", 2) match {
          case Array(paramName, paramDescription) =>
            Some(paramDescription)
          case Array(paramName) =>
            None
        }
      }
    }
  }
}
