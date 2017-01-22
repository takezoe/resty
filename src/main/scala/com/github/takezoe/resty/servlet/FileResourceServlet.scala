package com.github.takezoe.resty.servlet

import java.io.{FileInputStream, InputStream}

/**
 * A base class for servlets that provide resources on the file system as web contents.
 */
abstract class FileResourceServlet(basePath: String) extends ResourceServlet(basePath) {

  override protected def getResource(path: String): InputStream = {
    new FileInputStream(basePath + path)
  }

}
