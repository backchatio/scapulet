package io.backchat.scapulet

import java.net.{ ServerSocket }

object FreePort {

  def apply(maxRetries: Int = 50) = {
    val s = new ServerSocket(0)
    try { s.getLocalPort } finally { s.close() }
  }
}

