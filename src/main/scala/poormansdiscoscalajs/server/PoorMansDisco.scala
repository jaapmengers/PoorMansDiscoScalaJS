package poormansdiscoscalajs.server

import scala.scalajs.js
import scala.scalajs.js.JSApp
import monifu.reactive._
import monifu.concurrent.Scheduler
import monifu.concurrent.Implicits.globalScheduler
import scala.scalajs.js.annotation.JSExport

@JSExport
case class Event(val deltaTime: Double, val message: Int)

case class GetServerTimeResponse(val timestamp: Long)

object Formatters {
  implicit def unitReader(input: js.Dynamic) = ()
  implicit def stringWriter(input: String) = js.Any.fromString(input)
  implicit def getServerTimeResponseWriter(input: GetServerTimeResponse) = js.Dynamic.literal("timestamp" -> input.timestamp)
}

object ExpressWrapper {
  import Formatters.unitReader

  def get[U](path:String)(callback: () => U)(implicit expressAp: js.Dynamic, writer: U => js.Any): Unit = {
    get[Unit, U](path)(_ => callback())
  }

  def get[T, U](path: String)(callback: T => U)(implicit expressAp: js.Dynamic, reader: js.Dynamic => T, writer: U => js.Any): Unit = {

    val marshall: js.Function2[js.Dynamic, js.Dynamic, js.Dynamic] = { (req: js.Dynamic, resp: js.Dynamic) =>
      val result = callback(reader(req.params))
      resp.send(writer(result))
    }

    expressAp.get(path, marshall)
  }
}

object PoorMansDisco extends JSApp {
  import Formatters.getServerTimeResponseWriter

  implicit val expressApp = js.Dynamic.global.app

  // Handle the GetServerTime-call that is send as a classic HTTP-call
  ExpressWrapper.get[GetServerTimeResponse]("/getServerTime"){ () =>
    GetServerTimeResponse(System.currentTimeMillis())
  }

  // Handle incoming MIDI messages
  def messages(implicit scheduler: Scheduler): Observable[Event] =
    Observable.create { o =>
      js.Dynamic.global.eventreceived = (m:Event) => m match {
        // 248 is the code for a timecode signal
        case x: Event if x.message == 248 => o.onNext(x)
      }
    }

  // Summarize MIDI messages and forward them over a websocket to all connected clients
  def main(): Unit = {
    messages
      .map(_.deltaTime)
      .buffer(48)
      .map(x => x.sum / x.length)
      .map(x => 1/x*10)
      .foreach(x => js.Dynamic.global.sendMessage("something", x))
  }
}