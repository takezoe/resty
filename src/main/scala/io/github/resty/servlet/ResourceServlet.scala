package io.github.resty.servlet

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.apache.commons.io.IOUtils

abstract class ResourceServlet(basePath: String) extends HttpServlet {

  protected override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val resourcePath = request.getRequestURI.substring(request.getServletPath.length)
    val path = if(resourcePath.endsWith("/")) resourcePath + "index.html" else resourcePath

    val in = Thread.currentThread.getContextClassLoader.getResourceAsStream(basePath + path)
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
