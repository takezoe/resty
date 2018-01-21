package com.github.takezoe.resty

import okhttp3.{OkHttpClient, Request}
import org.scalatest._

import scala.collection.mutable.ListBuffer

class RandomRequestTargetSpec extends FunSuite {

  test("selects a url from given urls randomly"){
    val urls = Seq(
      "http://localhost:8080",
      "http://localhost:8081",
      "http://localhost:8082"
    )

    val result = new ListBuffer[Option[String]]()

    val target = new RandomRequestTarget(urls, HttpClientConfig()){
      override def execute[T](httpClient: OkHttpClient, configurer: (String, Request.Builder) => Unit, clazz: Class[_]): Either[ErrorModel, T] = {
        result += nextTarget.map { case target: SimpleRequestTarget => target.url }
        null
      }
    }

    for(i <- 1 to 100){
      target.execute(null, null, null)
    }

    val count1 = result.filter(_ == Some("http://localhost:8080")).length
    val count2 = result.filter(_ == Some("http://localhost:8081")).length
    val count3 = result.filter(_ == Some("http://localhost:8082")).length

    assert(count1 >= 20 && count1 <= 40)
    assert(count2 >= 20 && count2 <= 40)
    assert(count3 >= 20 && count3 <= 40)
    assert(count1 + count2 + count3 == 100)
  }

}