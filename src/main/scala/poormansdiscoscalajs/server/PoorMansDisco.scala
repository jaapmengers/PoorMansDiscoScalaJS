package poormansdiscoscalajs.server

import poormansdiscoscalajs.shared._
import scala.scalajs.js
import scala.scalajs.js.JSApp
import monifu.reactive._
import monifu.concurrent.Scheduler
import monifu.concurrent.Implicits.globalScheduler
import poormansdiscoscalajs.shared.BeatDelta
import poormansdiscoscalajs.shared.GetServerTimeResponse
import scala.concurrent.duration.FiniteDuration

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
  import poormansdiscoscalajs.shared.Formatters.{beatDeltaFormatter, serverTimeResponseFormatter, filterEventFormatter}

  implicit val expressInstance = ExpressInstance(js.Dynamic.global.app)
  implicit val socketInstance = SocketInstance(js.Dynamic.global.sendMessage)

  // Handle the GetServerTime-call that is send as a classic HTTP-call
  ExpressWrapper.get[GetServerTimeResponse]("/getServerTime"){ () =>
    GetServerTimeResponse(System.currentTimeMillis())
  }

  // Summarize MIDI messages and forward them over a websocket to all connected clients
  def main(): Unit = {

    // HACK! Monifu won't play nice when I try to split an Observable of a certain
    // basetrait into two observables of a specific type. So we create two seperate observables
    // based on function calls and this is the only way I could think of
    var registerBeatEvent: BeatEvent => Unit = (e: BeatEvent) => ()
    var registerFilterEvent: FilterEvent => Unit = (e: FilterEvent) => ()

    val beats = Observable.create { o =>
      registerBeatEvent = (e: BeatEvent) => o.observer.onNext(e)
    }: Observable[BeatEvent]

    val filters = Observable.create { o =>
      registerFilterEvent = (e: FilterEvent) => o.observer.onNext(e)
    }: Observable[FilterEvent]

    js.Dynamic.global.eventreceived = (m: MidiEvent) => {
      m.message.toArray match {
        case Array(248) => registerBeatEvent(BeatEvent(m.deltaTime))
        case Array(176, _, x) => registerFilterEvent(FilterEvent(x))
      }
    }

    beats.map(_.deltaTime)
      .buffer(48)
      .map(x => x.sum / x.length)
      .map(x => (1/x)*10)
      .foreach { x =>
      println(x)
      SocketWrapper.emit(BeatDelta(x, System.currentTimeMillis()))
    }

    filters.foreach(SocketWrapper.emit)
  }
}