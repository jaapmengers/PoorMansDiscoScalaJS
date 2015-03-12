package poormansdiscoscalajs.server

import poormansdiscoscalajs.shared.{Formatter, BeatDelta, Event, GetServerTimeResponse}
import scala.scalajs.js
import scala.scalajs.js.JSApp
import monifu.reactive._
import monifu.concurrent.Scheduler
import monifu.concurrent.Implicits.globalScheduler

case class ExpressInstance(val dynamic: js.Dynamic)
case class SocketInstance(val dynamic: js.Dynamic)

object ExpressWrapper {
  import poormansdiscoscalajs.shared.Formatters.unitFormatter

  def get[U](path:String)(callback: () => U)(implicit expressInstance: ExpressInstance, formatter: Formatter[U]): Unit = {
    get[Unit, U](path)(_ => callback())
  }

  def get[T, U](path: String)(callback: T => U)(implicit expressInstance: ExpressInstance, formatterT: Formatter[T], formatterU: Formatter[U]): Unit = {

    val marshall: js.Function2[js.Dynamic, js.Dynamic, js.Dynamic] = { (req: js.Dynamic, resp: js.Dynamic) =>
      val result = callback(formatterT.fromJsDynamic(req.params))
      resp.send(formatterU.toJsDynamic(result))
    }

    expressInstance.dynamic.get(path, marshall)
  }
}

object SocketWrapper {
  def emit[T](message: T)(implicit socketInstance: SocketInstance, formatter: Formatter[T]): Unit = {
    socketInstance.dynamic("cmd", formatter.toJsDynamic(message))
  }
}

object PoorMansDisco extends JSApp {
  import poormansdiscoscalajs.shared.Formatters.{beatDeltaFormatter, serverTimeResponseFormatter}

  implicit val expressInstance = ExpressInstance(js.Dynamic.global.app)
  implicit val socketInstance = SocketInstance(js.Dynamic.global.sendMessage)

  // Handle the GetServerTime-call that is send as a classic HTTP-call
  ExpressWrapper.get[GetServerTimeResponse]("/getServerTime"){ () =>
    GetServerTimeResponse(System.currentTimeMillis())
  }

  // Handle incoming MIDI messages
  def messages(implicit scheduler: Scheduler): Observable[Event] =
    Observable.create { o =>
      js.Dynamic.global.eventreceived = (m:Event) => m match {
        // 248 is the code for a timecode signal
        case x: Event if x.message == 248 => o.observer.onNext(x)
      }
    }

  // Summarize MIDI messages and forward them over a websocket to all connected clients
  def main(): Unit = {
    messages
      .map(_.deltaTime)
      .buffer(48)
      .map(x => x.sum / x.length)
      .map(x => (1/x)*10)
      .foreach(x => SocketWrapper.emit(BeatDelta(x, System.currentTimeMillis())))
  }
}