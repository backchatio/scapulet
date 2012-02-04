package io.backchat.scapulet

class ScapuletException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(msg: String) = this(msg, null)

  def this(cause: Throwable) = this(cause.getMessage, cause)

  def this() = this("There was an error")
}

class UnauthorizedException(msg: String) extends ScapuletException(msg)

class SASLAuthenticationFailed(e: Throwable) extends ScapuletException("SASL authentication failed!", e)

class NotSupportedException extends ScapuletException("Not supported")
