package com.github.takezoe.resty.servlet

import javax.servlet.{ServletRequest, ServletResponse}
import javax.servlet.annotation.WebServlet
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.github.takezoe.resty.{CORSSupport, RestyKernel}

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

  protected override def doOptions(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processAction(request, response, "option")
  }


  override def service(req: ServletRequest, res: ServletResponse): Unit = {
    val request = req.asInstanceOf[HttpServletRequest]
    val response = res.asInstanceOf[HttpServletResponse]

    if(CORSSupport.isCORSRequest(request)){
      CORSSupport.getAllowedOrigin(request).foreach { origin =>
        if(CORSSupport.isPreflightRequest(request)){
          CORSSupport.setCORSResponseHeaders(response, true, origin)
          return
        } else {
          CORSSupport.setCORSResponseHeaders(response, false, origin)
        }
      }
    }

    super.service(req, res)
  }

}
