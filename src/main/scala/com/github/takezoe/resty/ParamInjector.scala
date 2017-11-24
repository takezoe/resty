package com.github.takezoe.resty

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}

class ParamInjector {

  private val requestHolder = new ThreadLocal[HttpServletRequest]
  private val responseHolder = new ThreadLocal[HttpServletResponse]

  def withValues[T](request: HttpServletRequest, response: HttpServletResponse)(f: => T): T = {
    requestHolder.set(request)
    responseHolder.set(response)
    try {
      f
    } finally {
      requestHolder.remove()
      responseHolder.remove()
    }
  }

  def get(clazz: Class[_]): AnyRef = {
    if     (clazz == classOf[HttpServletRequest] ) requestHolder.get
    else if(clazz == classOf[HttpServletResponse]) responseHolder.get
    else if(clazz == classOf[HttpSession]        ) requestHolder.get.getSession
    else if(clazz == classOf[ServletContext]     ) requestHolder.get.getServletContext
    else throw new MatchError(s"${clazz.getName} isn't injectable.")
  }

}

object ParamInjector {

  def isInjectable(clazz: Class[_]): Boolean = {
    clazz == classOf[HttpServletRequest] ||
      clazz == classOf[HttpServletResponse] ||
      clazz == classOf[HttpSession] ||
      clazz == classOf[ServletContext]
  }

}
