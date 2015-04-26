package poormansdiscoscalajs.server

import importedjs.{Midi, Request, Express}
import importedjs.socketio.server.socketio
import monifu.reactive.channels.PublishChannel
import poormansdiscoscalajs.shared._
import scala.scalajs.js
import scala.scalajs.js.{JSON, JSApp}
import monifu.reactive._
import monifu.concurrent.Scheduler
import monifu.concurrent.Implicits.globalScheduler
import poormansdiscoscalajs.shared.BeatDelta
import poormansdiscoscalajs.shared.GetServerTimeResponse
import upickle._

import scala.util.{Success, Failure, Try}

object ExpressWrapper {
  import poormansdiscoscalajs.shared.Formatters.unitFormatter

  def get[U](path:String)(callback: () => U)(implicit formatter: Formatter[U]): Unit = {
    get[Unit, U](path)(_ => callback())
  }

  def get[T, U](path: String)(callback: T => U)(implicit formatterT: Formatter[T], formatterU: Formatter[U]): Unit = {

    val marshall = { (req: js.Dynamic, resp: js.Dynamic) =>
      val result = formatterT.fromJsDynamic(req.params).map(callback(_))
      if(result.isSuccess)
        resp.send(formatterU.toJsDynamic(result.get))
      else
        ()
    }

    Express.app.get(path, marshall)
  }
}

object SocketWrapper {
  val socketManager = socketio.SocketManager

  def emit[T](message: T)(implicit formatter: Formatter[T]): Unit = {
    // emit message over socket. Clients listen for messages with name "cmd"
  }
}

object PoorMansDisco extends JSApp {
  import poormansdiscoscalajs.shared.Formatters.{beatDeltaFormatter, serverTimeResponseFormatter, filterEventFormatter}

  // Handle the GetServerTime-call that is send as a classic HTTP-call
  ExpressWrapper.get[GetServerTimeResponse]("/getServerTime"){ () =>
    GetServerTimeResponse(System.currentTimeMillis())
  }

  Midi.midiInput.on("message", { (timestamp: Double, message: js.Array[Int]) =>
    eventReceived(MidiEvent(timestamp, message))
  })

  val channel = PublishChannel[Event]()

  def eventReceived(m: MidiEvent) = {
    // Match on midiEvent. Beat is 248, Filter is 176
  }

  // Summarize MIDI messages and forward them over a websocket to all connected clients
  def main(): Unit = {
    val beats = channel.collect {
      case b: BeatEvent => b
    }

    val filters = channel.collect {
      case f: FilterEvent => f
    }

    beats.map(_.deltaTime)
      .buffer(24)
      .map(x => x.sum / x.length)
      .map(x => (1/x)*10)
      .foreach { x =>
      SocketWrapper.emit(BeatDelta(x, System.currentTimeMillis()))
    }

    filters.foreach(SocketWrapper.emit)
  }
}