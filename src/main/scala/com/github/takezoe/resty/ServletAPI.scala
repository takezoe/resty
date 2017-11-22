package com.github.takezoe.resty

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait ServletAPI {

  private[resty] val requestHolder = new ThreadLocal[HttpServletRequest]
  private[resty] val responseHolder = new ThreadLocal[HttpServletResponse]

  private[resty] def withValues[T](request: HttpServletRequest, response: HttpServletResponse)(f: => T): T = {
    requestHolder.set(request)
    responseHolder.set(response)
    try {
      f
    } finally {
      requestHolder.remove()
      responseHolder.remove()
    }
  }

  def request: HttpServletRequest = requestHolder.get()
  def response: HttpServletResponse = responseHolder.get()

}
