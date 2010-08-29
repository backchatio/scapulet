package com.mojolly.scapulet

object Exceptions {

  class ScapuletException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
    def this(msg: String) = this(msg, null)
    def this(cause: Throwable) = this(cause.getMessage, cause)
    def this() = this("There was an error")
  }
  class UnauthorizedException(msg: String) extends ScapuletException(msg)
}