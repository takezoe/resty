package com.github.takezoe.resty.servlet

import javax.servlet._
import javax.servlet.annotation.WebFilter

import com.github.kristofa.brave.servlet.BraveServletFilter
import com.github.takezoe.resty.ZipkinSupport

@WebFilter(urlPatterns = Array("/*"))
class ZipkinBraveFilter extends Filter {

  protected val filter = BraveServletFilter.create(ZipkinSupport.brave)

  override def init(filterConfig: FilterConfig): Unit = filter.init(filterConfig)

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = filter.doFilter(request, response, chain)

  override def destroy(): Unit = {}

}
