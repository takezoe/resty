package com.github.takezoe.resty.util

import java.lang.reflect.Method

import org.scalatest._

class ReflectionUtilsTest {
  def sequence(): Seq[String] = Nil
}

class ReflectionUtilsSpec extends FunSuite {


  test("Get generic type of method return type"){
    val clazz = classOf[ReflectionUtilsTest]
    val method = getMethod(clazz, "sequence")
    val result = ReflectionUtils.getWrappedTypeOfMethod(method)

    assert(result == Some(classOf[String]))
  }

  private def getMethod(clazz: Class[_], name: String): Method = {
    clazz.getDeclaredMethods.find(_.getName == name).get
  }

}
