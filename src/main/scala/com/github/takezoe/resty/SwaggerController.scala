package com.github.takezoe.resty

import java.io.{File, InputStream}
import java.lang.reflect.{Field, Method}

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.github.takezoe.resty.model.ParamConverter.JsonConverter
import com.github.takezoe.resty.model.ParamDef
import com.github.takezoe.resty.util.ReflectionUtils
import io.swagger.models._
import io.swagger.models.parameters._
import io.swagger.models.properties._

import scala.collection.mutable
import scala.concurrent.Future

/**
 * Endpoint that provides Swagger 2.0 JSON.
 */
class SwaggerController {

  @Action(method = "GET", path = "/swagger.json")
  def swaggerJson() = {
    val swagger = new Swagger()

    val appInfo = Resty.appInfo
    if(appInfo.nonEmpty){
      val info = new Info()
      if(appInfo.title.nonEmpty){ info.setTitle(appInfo.title) }
      if(appInfo.version.nonEmpty){ info.setVersion(appInfo.version) }
      if(appInfo.description.nonEmpty){ info.setDescription(appInfo.description) }
      swagger.setInfo(info)
    }

    val paths = new mutable.HashMap[String, Path]()
    val models = new mutable.HashMap[String, Model]()
    val tags = new mutable.HashMap[String, Tag]()

    Resty.allActions.filterNot(_._1.instance.isInstanceOf[SwaggerController]).foreach { case (controller, action) =>
      val tag = new Tag()
      val tagName = if(controller.name.nonEmpty) controller.name else controller.instance.getClass.getSimpleName
      tag.setName(tagName)
      if(controller.description.nonEmpty){ tag.setDescription(controller.description) }
      tags.put(tag.getName, tag)

      val path = paths.getOrElseUpdate(action.path, new Path())
      val operation = new Operation()
      operation.setOperationId(action.function.getName)
      operation.addTag(tagName)

      if(action.description.nonEmpty){
        operation.setDescription(action.description)
      }
      if(action.deprecated){
        operation.setDeprecated(true)
      }

      action.params.foreach { paramDef =>
        paramDef match {
          case ParamDef.PathParam(name, description, converter) =>
            val parameter = new PathParameter()
            if(description.nonEmpty){ parameter.setDescription(description) }
            operation.addParameter(converter.parameter(parameter))

          case ParamDef.QueryParam(name, description, converter) =>
            val parameter = new QueryParameter()
            if(description.nonEmpty){ parameter.setDescription(description) }
            operation.addParameter(converter.parameter(parameter))

          case ParamDef.HeaderParam(name, description, converter) =>
            val parameter = new HeaderParameter()
            if(description.nonEmpty){ parameter.setDescription(description) }
            operation.addParameter(converter.parameter(parameter))

          case ParamDef.BodyParam(name, description, clazz, converter) =>
            val parameter = new BodyParameter()
            if(description.nonEmpty){ parameter.setDescription(description) }
            operation.addParameter(converter.parameter(parameter))
            if(converter.isInstanceOf[JsonConverter]){
              models.put(clazz.getSimpleName, createModel(action.function, clazz, models))
            }
          case ParamDef.InjectParam(_, _, _, _) => // Ignore inject parameter
        }
      }

      {
        val response = new Response()
        createProperty(action.function, models).map { property =>
          response.setSchema(property)
        }
        operation.addResponse("200", response)
      }

      {
        val returnType = classOf[ErrorModel]
        val response = new Response()
        response.setSchema(new RefProperty(returnType.getSimpleName))
        models.put(returnType.getSimpleName, createModel(action.function, returnType, models))
        operation.addResponse("default", response)
        operation.produces("application/json")
      }

      action.method match {
        case "get"    => path.get(operation)
        case "post"   => path.post(operation)
        case "put"    => path.put(operation)
        case "delete" => path.delete(operation)
      }
    }

    tags.foreach { case (key, tag) => swagger.addTag(tag) }
    paths.foreach { case (key, path) => swagger.path(key, path) }
    models.foreach { case (key, model) => swagger.addDefinition(key, model) }

    swagger
  }

  protected def createModel(actionMethod: Method, clazz: Class[_], models: mutable.HashMap[String, Model]): Model = {
    // TODO: Jackson can not deserialize Seq[T] in default...
    if(clazz == classOf[Seq[_]]) {
      val model = new ArrayModel()
      ReflectionUtils.getWrappedTypeOfMethodArgument(actionMethod, 0).foreach { wrappedType =>
        createSimpleProperty(actionMethod, wrappedType, models).foreach { wrappedProperty =>
          model.setItems(wrappedProperty)
        }
      }
      model
    } else if(clazz.isArray){
      val model = new ArrayModel()
      createSimpleProperty(actionMethod, clazz.getComponentType, models).foreach { wrappedProperty =>
        model.setItems(wrappedProperty)
      }
      model
    } else {
      val model = new ModelImpl()
      model.setName(clazz.getSimpleName)

      clazz.getDeclaredFields.foreach { field =>
        if(field.getName != "$outer"){
          createProperty(actionMethod, field, models).foreach { property =>
            val param = clazz.getConstructors.head.getParameters.find(_.getName == field.getName)

            val ignore =
            // Check @JsonIgnoreProperties
              Option(clazz.getAnnotation(classOf[JsonIgnoreProperties])).map { a =>
                a.value().contains(field.getName)
              }.getOrElse(false) //|| // TODO jackson-module-scala does not seem to support @JsonIgnore
            //            // Check @JsonIgnore
            //            param.flatMap { param =>
            //              Option(param.getAnnotation(classOf[JsonIgnore])).map { a =>
            //                a.value()
            //              }
            //            }.getOrElse(false)

            if(!ignore){
              val propertyName = param.flatMap { param =>
                Option(param.getAnnotation(classOf[JsonProperty])).map { a =>
                  a.value()
                }
              }.getOrElse(field.getName)

              model.addProperty(propertyName, property)
            }
          }
        }
      }

      model
    }
  }

  protected def createProperty(actionMethod: Method, models: mutable.HashMap[String, Model]): Option[Property] = {
    val fieldType = actionMethod.getReturnType

    // TODO Map support?
    if(fieldType == classOf[Option[_]]){
      ReflectionUtils.getWrappedTypeOfMethod(actionMethod).flatMap { wrappedType =>
        createSimpleProperty(actionMethod, wrappedType, models)
      }
    } else if(fieldType == classOf[Seq[_]]) {
      ReflectionUtils.getWrappedTypeOfMethod(actionMethod).map { wrappedType =>
        val property = new ArrayProperty()
        createSimpleProperty(actionMethod, wrappedType, models).foreach { wrappedProperty =>
          property.setItems(wrappedProperty)
        }
        property
      }
    } else if(fieldType == classOf[ActionResult[_]]){
      ReflectionUtils.getWrappedTypeOfMethod(actionMethod).flatMap { wrappedType =>
        createSimpleProperty(actionMethod, wrappedType, models)
      }
    } else if(fieldType == classOf[Future[_]]){
      // TODO When wrapped type is ActionResult...?
      ReflectionUtils.getWrappedTypeOfMethod(actionMethod).flatMap { wrappedType =>
        createSimpleProperty(actionMethod, wrappedType, models)
      }
    } else {
      createSimpleProperty(actionMethod, fieldType, models).map { property =>
        property.setRequired(true)
        property
      }
    }
  }

  protected def createProperty(actionMethod: Method, field: Field, models: mutable.HashMap[String, Model]): Option[Property] = {
    val fieldType = field.getType

    // TODO Map support?
    if(fieldType == classOf[Option[_]]){
      ReflectionUtils.getWrappedTypeOfField(field).flatMap { wrappedType =>
        createSimpleProperty(actionMethod, wrappedType, models)
      }
    } else if(fieldType == classOf[Seq[_]]){
        ReflectionUtils.getWrappedTypeOfField(field).map { wrappedType =>
          val property = new ArrayProperty()
          createSimpleProperty(actionMethod, wrappedType, models).foreach { wrappedProperty =>
            property.setItems(wrappedProperty)
          }
          property
        }
    } else {
      createSimpleProperty(actionMethod, fieldType, models).map { property =>
        property.setRequired(true)
        property
      }
    }
  }

  protected def createSimpleProperty(actionMethod: Method, clazz: Class[_], models: mutable.HashMap[String, Model]): Option[Property] = {
    if(clazz == classOf[String]){
      Some(new StringProperty())
    } else if(clazz == classOf[Int]){
      Some(new IntegerProperty())
    } else if(clazz == classOf[Long]) {
      Some(new LongProperty())
    } else if(clazz == classOf[Double]) {
      Some(new DoubleProperty())
    } else if(clazz == classOf[Boolean]) {
      Some(new BooleanProperty())
    } else if(clazz.isArray && clazz.getComponentType == classOf[Byte]){
      Some(new ByteArrayProperty())
    } else if(clazz == classOf[File] || clazz == classOf[InputStream]){
      Some(new FileProperty())
    } else if(clazz == classOf[Unit]){
      None
    } else {
      models.put(clazz.getSimpleName, createModel(actionMethod, clazz, models))
      Some(new RefProperty(clazz.getSimpleName))
    }
  }

}
