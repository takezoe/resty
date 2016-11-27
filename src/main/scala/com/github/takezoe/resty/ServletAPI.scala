package com.github.takezoe.resty

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait ServletAPI {

  private[resty] val requestHolder = new ThreadLocal[HttpServletRequest]
  private[resty] val responseHolder = new ThreadLocal[HttpServletResponse]

  def request: HttpServletRequest = requestHolder.get()
  def response: HttpServletResponse = responseHolder.get()

}
