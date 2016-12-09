package com.github.takezoe.resty

case class ErrorModel(errors: Seq[String])

case class ActionResult[T](status: Int, body: T, headers: Map[String, String] = Map.empty){
  def withHeaders(headers: (String, String)*): ActionResult[T] = {
    copy(headers = headers.toMap)
  }
}

object Ok {
  def apply(): ActionResult[Unit]        = ActionResult[Unit](200, ())
  def apply[T](body: T): ActionResult[T] = ActionResult[T](200, body)
}

object BadRequest {
  def apply[T](): T             = throw new ActionResultException(ActionResult(400, ()))
  def apply[T](body: AnyRef): T = throw new ActionResultException(ActionResult(400, body))
}


object NotFound {
  def apply[T](): T             = throw new ActionResultException(ActionResult(404, ()))
  def apply[T](body: AnyRef): T = throw new ActionResultException(ActionResult(404, body))
}

object InternalServerError {
  def apply[T](): T             = throw new ActionResultException(ActionResult(500, ()))
  def apply[T](body: AnyRef): T = throw new ActionResultException(ActionResult(500, body))
}

class ActionResultException(val result: ActionResult[_]) extends RuntimeException(result.body.toString)