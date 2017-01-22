package com.github.takezoe.resty.servlet

import java.io.InputStream
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.apache.commons.io.IOUtils
import scala.util.control.Exception

/**
 * A base class for servlets that provide resources on the classpath as web contents.
 */
abstract class ResourceServlet(basePath: String) extends HttpServlet {

  protected override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val resourcePath = request.getRequestURI.substring(request.getServletPath.length)
    val path = if(resourcePath.endsWith("/")) resourcePath + "index.html" else resourcePath

    val in = getResource(path)
    if(in != null){
      try {
        val content = IOUtils.toByteArray(in)
        val out = response.getOutputStream

        response.setContentType(getContentType(path))
        response.setContentLength(content.length)
        out.write(content)

      } finally {
        Exception.ignoring(classOf[Exception]){
          in.close()
        }
      }
    }
  }

  protected def getResource(path: String): InputStream = {
    Thread.currentThread.getContextClassLoader.getResourceAsStream(basePath + path)
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
