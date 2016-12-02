package com.github.takezoe.resty.util

import java.lang.reflect.{Field, Method}

import org.scalatest._

case class ReflectionUtilsTest1(optionField: Option[String], seqField: Seq[String]) {
  def optionMethod(): Option[String] = None
  def seqMethod(): Seq[String] = Nil
}

class ReflectionUtilsSpec extends FunSuite {

  case class ReflectionUtilsTest2(optionField: Option[String], seqField: Seq[String]) {
    def optionMethod(): Option[String] = None
    def seqMethod(): Seq[String] = Nil
  }

  test("Get generic type of method return type"){
    val clazz = classOf[ReflectionUtilsTest1]

    {
      val method = getMethod(clazz, "optionMethod")
      val result = ReflectionUtils.getWrappedTypeOfMethod(method)

      assert(result == Some(classOf[String]))
    }

    {
      val method = getMethod(clazz, "seqMethod")
      val result = ReflectionUtils.getWrappedTypeOfMethod(method)

      assert(result == Some(classOf[String]))
    }
  }

  test("Get generic type of field type"){
    val clazz = classOf[ReflectionUtilsTest1]

    {
      val field = getField(clazz, "optionField")
      val result = ReflectionUtils.getWrappedTypeOfField(field)

      assert(result == Some(classOf[String]))
    }

    {
      val field = getField(clazz, "seqField")
      val result = ReflectionUtils.getWrappedTypeOfField(field)

      assert(result == Some(classOf[String]))
    }
  }

  test("Get generic type of method type of inner class"){
    val clazz = classOf[ReflectionUtilsTest2]

    {
      val method = getMethod(clazz, "optionMethod")
      val result = ReflectionUtils.getWrappedTypeOfMethod(method)

      assert(result == Some(classOf[String]))
    }

    {
      val method = getMethod(clazz, "seqMethod")
      val result = ReflectionUtils.getWrappedTypeOfMethod(method)

      assert(result == Some(classOf[String]))
    }
  }

  test("Get generic type of field type of inner class"){
    val clazz = classOf[ReflectionUtilsTest2]

    {
      val field = getField(clazz, "optionField")
      val result = ReflectionUtils.getWrappedTypeOfField(field)

      assert(result == Some(classOf[String]))
    }

    {
      val field = getField(clazz, "seqField")
      val result = ReflectionUtils.getWrappedTypeOfField(field)

      assert(result == Some(classOf[String]))
    }
  }

  private def getField(clazz: Class[_], name: String): Field = clazz.getDeclaredFields.find(_.getName == name).get
  private def getMethod(clazz: Class[_], name: String): Method = clazz.getDeclaredMethods.find(_.getName == name).get

}
