package com.github.takezoe.resty.servlet

import java.io.InputStream

import org.webjars.WebJarAssetLocator

class WebJarsServlet extends ResourceServlet("") {

  private val locator = new WebJarAssetLocator()

  override protected def getResource(path: String): InputStream = {
    Thread.currentThread.getContextClassLoader.getResourceAsStream(locator.getFullPath(path))
  }

}
