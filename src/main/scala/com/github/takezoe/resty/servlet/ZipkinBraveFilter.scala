package com.github.takezoe.resty.servlet

import javax.servlet._

import brave.servlet.TracingFilter
import com.github.takezoe.resty.HttpClientSupport

class ZipkinBraveFilter extends Filter {

  protected val filter = TracingFilter.create(HttpClientSupport.tracing)

  override def init(filterConfig: FilterConfig): Unit = filter.init(filterConfig)

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = filter.doFilter(request, response, chain)

  override def destroy(): Unit = {}

}
