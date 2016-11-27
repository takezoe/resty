package io.github.takezoe.resty.util

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule
//import com.fasterxml.jackson.databind.module.SimpleModule
//import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, Version}
//import org.joda.time.DateTime
//import org.joda.time.format.DateTimeFormat
//import scala.reflect.ClassTag

object JsonUtils {

  val mapper = new ObjectMapper()
  mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
  mapper.setSerializationInclusion(Include.NON_NULL)
  mapper.registerModule(DefaultScalaModule)
  // TODO Support conversion for Java8 Date & Time API
//  mapper.registerModule(new SimpleModule("MyModule", Version.unknownVersion())
//    .addSerializer(classOf[DateTime], new JsonSerializer[DateTime] {
//      override def serialize(value: DateTime, generator: JsonGenerator, provider: SerializerProvider): Unit = {
//        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC()
//        generator.writeString(formatter.print(value))
//      }
//    })
//    .addDeserializer(classOf[DateTime], new JsonDeserializer[DateTime](){
//      override def deserialize(parser: JsonParser, context: DeserializationContext): DateTime = {
//        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC()
//        formatter.parseDateTime(if(parser.getValueAsString != null) parser.getValueAsString else parser.nextTextValue)
//      }
//    })
//  )

  def serialize(doc: AnyRef): String = mapper.writeValueAsString(doc)

  def deserialize(json: String, c: Class[_]): AnyRef = mapper.readValue(json, c).asInstanceOf[AnyRef]

}
