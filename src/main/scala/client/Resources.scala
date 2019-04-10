package client

import scala.util.control.NonFatal

object Resources {
  def tryWith[T <: AutoCloseable, V](r: => T)(f: T => V): V = {
    val io: T = r
    //require(io != null, "io is null")
    var exception: Throwable = null
    try {
      f(io)
    }
    catch {
      case NonFatal(e) =>
        exception = e
        throw e
    }
    finally {
      if (exception != null) {
        try {
          io.close
        }
        catch {
          case NonFatal(suppressed) => exception.addSuppressed(suppressed)
        }
      }
      else {
        io.close
      }
    }
  }
}
