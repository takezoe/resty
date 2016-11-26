package io.github.resty.model

case class AppInfo(title: String = "", version: String = "", description: String = ""){
  val isEmpty = title.isEmpty && version.isEmpty && description.isEmpty
  val nonEmpty = !isEmpty
}