package io.github.resty.model

import java.lang.reflect.Method

case class ActionDef(
  method: String,
  path: String,
  description: String,
  params: Seq[ParamDef],
  function: Method,
  controller: AnyRef
)

