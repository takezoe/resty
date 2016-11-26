package io.github.resty.servlet

import javax.servlet.annotation.WebServlet

@WebServlet(name="SwaggerUIServlet", urlPatterns=Array("/swagger-ui/*"))
class SwaggerUIServlet extends ResourceServlet("/public/vendors/swagger-ui")
