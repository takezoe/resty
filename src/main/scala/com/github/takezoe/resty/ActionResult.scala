package com.github.takezoe.resty

case class ErrorModel(errors: Seq[String])

case class ActionResult(status: Int, body: Option[AnyRef])

object Ok {
  def apply() = ActionResult(200, None)
  def apply(body: AnyRef) = ActionResult(200, Some(body))
}

object BadRequest {
  def apply() = ActionResult(400, None)
  def apply(body: AnyRef) = ActionResult(400, Some(body))
}

object NotFound {
  def apply() = ActionResult(404, None)
  def apply(body: AnyRef) = ActionResult(404, Some(body))
}

object InternalServerError {
  def apply() = ActionResult(500, None)
  def apply(body: AnyRef) = ActionResult(500, Some(body))
}
