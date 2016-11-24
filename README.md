Resty
========

Super easy REST API framework for Scala

Example
-------

This is a simplest controller example:

```scala
class HelloController {
  @Action(method = "GET", path = "/hello/{name}")
  def hello(name: String): Message = {
    Message(s"Hello ${name}!")
  }
}

case class Message(message: String)
```

Define a web listener that registers your controller.

```
@WebListener
class InitializeListener extends ServletContextListener {
  override def contextDestroyed(sce: ServletContextEvent): Unit = {
  }
  override def contextInitialized(sce: ServletContextEvent): Unit = {
    Resty.register(new HelloController())
  }
}
```

Let's test this controller.

```
curl -XGET http://localhost:8080/hello/resty
{"message": "Hello resty!" }
```

Resty provides Swagger integration in default. Swagger JSON is provided at `http://localhost:8080/swagger.json` and also Swagger UI is available at `http://localhost:8080/swagger-ui/`.



