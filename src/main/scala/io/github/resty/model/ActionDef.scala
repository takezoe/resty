package io.github.resty.model

import java.lang.reflect.Method

case class ActionDef(method: String, path: String, params: Seq[ParamDef], function: Method, controller: AnyRef)

