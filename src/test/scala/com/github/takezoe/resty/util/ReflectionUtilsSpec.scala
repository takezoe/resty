package com.github.takezoe.resty.util

import java.lang.reflect.{Field, Method}

import org.scalatest._

case class ReflectionUtilsTest1(optionField: Option[String], seqField: Seq[String], nestField: Option[ReflectionUtilsNestTest1]) {
  def optionMethod(): Option[String] = None
  def seqMethod(): Seq[String] = Nil
  def nestMethod(): Option[ReflectionUtilsNestTest1] = None
}

case class ReflectionUtilsNestTest1(field: String)

class ReflectionUtilsSpec extends FunSuite {

  case class ReflectionUtilsTest2(optionField: Option[String], seqField: Seq[String], nestField: Option[ReflectionUtilsNestTest2]) {
    def optionMethod(): Option[String] = None
    def seqMethod(): Seq[String] = Nil
    def nestMethod(): Option[ReflectionUtilsNestTest2] = None
  }
  case class ReflectionUtilsNestTest2(field: String)

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

    {
      val method = getMethod(clazz, "nestMethod")
      val result = ReflectionUtils.getWrappedTypeOfMethod(method)

      assert(result == Some(classOf[ReflectionUtilsNestTest1]))
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

    {
      val field = getField(clazz, "nestField")
      val result = ReflectionUtils.getWrappedTypeOfField(field)

      assert(result == Some(classOf[ReflectionUtilsNestTest1]))
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

    {
      val method = getMethod(clazz, "nestMethod")
      val result = ReflectionUtils.getWrappedTypeOfMethod(method)

      assert(result == Some(classOf[ReflectionUtilsNestTest2]))
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

    {
      val field = getField(clazz, "nestField")
      val result = ReflectionUtils.getWrappedTypeOfField(field)

      assert(result == Some(classOf[ReflectionUtilsNestTest2]))
    }
  }

  private def getField(clazz: Class[_], name: String): Field = clazz.getDeclaredFields.find(_.getName == name).get
  private def getMethod(clazz: Class[_], name: String): Method = clazz.getDeclaredMethods.find(_.getName == name).get

}
