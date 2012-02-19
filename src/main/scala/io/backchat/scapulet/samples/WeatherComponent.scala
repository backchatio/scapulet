package io.backchat.scapulet.samples

import io.backchat.scapulet.{ ConnectionConfig, ComponentConfig }

object WeatherComponent {

  val config = ComponentConfig(
    name = "Weather Demo Component",
    description = "A Demo for implementing an external XMPP component",
    connection = ConnectionConfig("weatherdemo", "weatherdemo", "127.0.0.1", 39473, Some("localhost")))



  def main(args: Array[String]) {
    println("Still needs to be implemented but this will hold the demo for the weather component")
  }
}
