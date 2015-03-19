package poormansdiscoscalajs.server

import importedjs.socketio.server.socketio
import monifu.reactive.channels.PublishChannel
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
  val socketManager = socketio.SocketManager

  def emit[T](message: T)(implicit socketInstance: SocketInstance, formatter: Formatter[T]): Unit = {
    socketManager.sockets.emit("cmd", formatter.toJsDynamic(message))
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

    val channel = PublishChannel[Event]()

    js.Dynamic.global.eventreceived = (m: MidiEvent) => {
      m.message.toArray match {
        case Array(248) => channel.pushNext(BeatEvent(m.deltaTime))
        case Array(176, _, x) => channel.pushNext(FilterEvent(x))
        case _ => //for now, we don't support any other events
      }
    }

    val beats = channel.collect {
      case b: BeatEvent => b
    }

    val filters = channel.collect {
      case f: FilterEvent => f
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