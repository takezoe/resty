package com.github.takezoe.resty.servlet

import javax.servlet.annotation.WebServlet
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.RestyKernel

@WebServlet(name="RestyServlet", urlPatterns=Array("/*"), asyncSupported = true)
class RestyServlet extends HttpServlet with RestyKernel {

  protected override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processAction(request, response, "get")
  }

  protected override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processAction(request, response, "post")
  }

  protected override def doPut(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processAction(request, response, "put")
  }

  protected override def doDelete(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processAction(request, response, "delete")
  }

}

