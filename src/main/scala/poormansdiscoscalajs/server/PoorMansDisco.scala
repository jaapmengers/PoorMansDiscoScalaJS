package poormansdiscoscalajs.server

import importedjs.{Request, Express}
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


trait LightOptions {
  def toSetting: Map[String, Double]
}

case class Saturation(val coefficient : Double) extends LightOptions {
  def toSetting = Map("sat" -> scalajs.js.Math.round(coefficient * 255))
}

case class Hue(val coefficient: Double) extends LightOptions {
  def toSetting = Map("sat" -> scalajs.js.Math.round(coefficient * 10000))
}

case class Brightness(val coefficient: Double) extends LightOptions {
  def toSetting = Map("bri" -> scalajs.js.Math.round(coefficient * 255))
}

object ExpressWrapper {
  import poormansdiscoscalajs.shared.Formatters.unitFormatter

  def get[U](path:String)(callback: () => U)(implicit formatter: Formatter[U]): Unit = {
    get[Unit, U](path)(_ => callback())
  }

  def get[T, U](path: String)(callback: T => U)(implicit formatterT: Formatter[T], formatterU: Formatter[U]): Unit = {

    val marshall: js.Function2[js.Dynamic, js.Dynamic, js.Dynamic] = { (req: js.Dynamic, resp: js.Dynamic) =>
      val result = callback(formatterT.fromJsDynamic(req.params))
      resp.send(formatterU.toJsDynamic(result))
    }

    Express.app.get(path, marshall)
  }
}

object BridgeAPI {
  def SetOption(options: LightOptions): Unit = {

    val pair = options.toSetting
    val requestObj = Map(
      "url" -> "http://192.168.0.19/api/newdeveloper/lights/1/state",
      "body" -> write(pair),
      "method" -> "PUT"
    )

    Request.requestInstance.put(JSON.parse(write(requestObj)), (err: js.Dynamic, response: js.Dynamic, body: js.Dynamic) => {
      // TODO: Response.statusCode is sometimes undefined, figure out if this can be fixed in types. Also, might wanna return a future
      println("Response received")
    })
  }
}

object SocketWrapper {
  val socketManager = socketio.SocketManager

  def emit[T](message: T)(implicit formatter: Formatter[T]): Unit = {
    socketManager.sockets.emit("cmd", formatter.toJsDynamic(message))
  }
}

object PoorMansDisco extends JSApp {
  import poormansdiscoscalajs.shared.Formatters.{beatDeltaFormatter, serverTimeResponseFormatter, filterEventFormatter}

  // Handle the GetServerTime-call that is send as a classic HTTP-call
  ExpressWrapper.get[GetServerTimeResponse]("/getServerTime"){ () =>
    GetServerTimeResponse(System.currentTimeMillis())
  }

  def toLightOptions(fe: FilterEvent): Option[LightOptions] = {
    val coefficient = fe.filterItensity / 127.0

    fe.which match {
      case 0 => Some(Saturation(coefficient))
      case 1 => Some(Hue(coefficient))
      case 2 => Some(Brightness(coefficient))
      case _ => None
    }
  }

  // Summarize MIDI messages and forward them over a websocket to all connected clients
  def main(): Unit = {
    val channel = PublishChannel[Event]()

    js.Dynamic.global.eventreceived = (m: MidiEvent) => {
      m.message.toArray match {
        case Array(248) => channel.pushNext(BeatEvent(m.deltaTime))
        case Array(176, which, intensity) => channel.pushNext(FilterEvent(which, intensity))
        case _ => println("Other")
      }
    }

    val beats = channel.collect {
      case b: BeatEvent => b
    }

    val filters = channel.collect {
      case f: FilterEvent => toLightOptions(f)
    }.collect {
      case Some(lo: LightOptions) => lo
    }

    beats.map(_.deltaTime)
      .buffer(48)
      .map(x => x.sum / x.length)
      .map(x => (1/x)*10)
      .foreach { x =>
      println(x)
      SocketWrapper.emit(BeatDelta(x, System.currentTimeMillis()))
    }

    filters.foreach(BridgeAPI.SetOption)
  }
}