package io.github.resty.servlet

import javax.servlet.annotation.WebServlet
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.apache.commons.io.IOUtils

@WebServlet(name="ResourceServlet", urlPatterns=Array("/swagger-ui/*"))
class ResourceServlet extends HttpServlet {

  private val basePath = "/swagger-ui"

  protected override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val path = if(request.getRequestURI.endsWith("/")) request.getRequestURI + "index.html" else request.getRequestURI

    val in = Thread.currentThread.getContextClassLoader.getResourceAsStream("/public/vendors" + path)
    if(in != null){
      val content = IOUtils.toByteArray(in)
      val out = response.getOutputStream

      response.setContentType(getContentType(path))
      response.setContentLength(content.length)
      out.write(content)
    }
  }

  protected def getContentType(path: String): String = {
    if(path.endsWith(".html")){
      "text/html; charset=UTF-8"
    } else if(path.endsWith(".css")){
      "text/css; charset=UTF-8"
    } else if(path.endsWith(".js")){
      "text/javascript; charset=UTF-8"
    } else if(path.endsWith(".png")){
      "image/png"
    } else {
      "application/octet-stream"
    }
  }


}
