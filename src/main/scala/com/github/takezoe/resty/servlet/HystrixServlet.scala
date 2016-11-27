package com.github.takezoe.resty.servlet

import javax.servlet.annotation.WebServlet

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsStreamServlet

@WebServlet(name="HystrixServlet", urlPatterns=Array("/hystrix.stream"))
class HystrixServlet extends HystrixMetricsStreamServlet
