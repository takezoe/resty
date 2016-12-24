package com.github.takezoe.resty.model

import java.lang.reflect.Method

case class ActionDef(
  method: String,
  path: String,
  description: String,
  deprecated: Boolean,
  params: Seq[ParamDef],
  function: Method
)

