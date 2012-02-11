package io.backchat.scapulet

import java.net.{ SocketException, ConnectException, Socket, ServerSocket }
import util.Random

object FreePort {

  def isPortFree(port: Int) = {
    try {
      val socket = new Socket("127.0.0.1", port)
      socket.close()
      false
    } catch {
      case e: ConnectException ⇒ true
      case e: SocketException if e.getMessage == "Connection reset by peer" ⇒ true
    }
  }

  private def newPort = Random.nextInt(55365) + 10000

  def apply(maxRetries: Int = 50) = {
    var count = 0
    var freePort = newPort
    while (!isPortFree(freePort)) {
      freePort = newPort
      count += 1
      if (count >= maxRetries) {
        throw new RuntimeException("Couldn't determine a free port")
      }
    }
    freePort
  }
}

