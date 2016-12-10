package com.github.takezoe.resty.util

object StringUtils {

  def trim(value: String): String = Option(value).map(_.trim).getOrElse("")

}
