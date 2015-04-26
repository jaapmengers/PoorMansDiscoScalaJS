package poormansdiscoscalajs.client

import importedjs.socketio.client.socketio
import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom
import poormansdiscoscalajs.shared._

import concurrent.duration._
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{JSON, Date, JSApp}
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react.{BackendScope, React, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.concurrent.Implicits.globalScheduler

import dom.ext.Ajax
import scala.util.{Failure, Success, Try}
import scalajs.concurrent
        .JSExecutionContext
        .Implicits
import poormansdiscoscalajs.shared.BeatDelta
import scala.Some
import poormansdiscoscalajs.shared.FilterEvent
import poormansdiscoscalajs.shared.Formatters.serverTimeResponseFormatter

object AjaxWrapper {
  def get[T](path: String)(implicit formatter: Formatter[T]): Future[Try[T]] = {
    for {
      response <- Ajax.get(path)
      parsed = JSON.parse(response.responseText)
    } yield formatter.fromJsDynamic(parsed)
  }
}

case class State(className: String, intensity: Int)

class Backend($: BackendScope[_, State], beatObs: Observable[Long], filterObs: Observable[FilterEvent]) {
  // Iterate over beat and filter events. On beat event, set state on, setTimeout for 100ms and set state off again
  // For filter events, set the intensity
}

object DiscoClient extends JSApp{

  @JSExport
  override def main(): Unit = {
    // Do getServerTime-call, unmarshall object, calculate timedifference and start listening for events
  }

  def getTimeDifference(requestDate: Double, responseDate: Double, serverDate: Double) = {
    val estimatedLatency = (responseDate - requestDate) / 2
    serverDate - requestDate - estimatedLatency
  }

  def switch[T](input: Observable[Observable[T]]): Observable[T] = {
    Observable.create { result =>
      var previous: Option[Observable[T]] = None
      input.foreach { innerObs =>
        previous = Some(innerObs)
        innerObs.foreach { item =>
          if(previous == Some(innerObs)){
            result.observer.onNext(item)
          }
        }
      }
    }
  }


  def getStyle(intensity: Int): List[TagMod] = {
    val coefficient = Math.abs(intensity - 63.0) / 63.0
    val p = Math.round(100.0 - (coefficient * 100.0))

    val value = s"saturate($p%)"

    List("filter".reactStyle := value,
      "WebkitFilter".reactStyle := value,
      "MozFilter".reactStyle := value,
      "OFilter".reactStyle := value,
      "msFilter".reactStyle := value)
  }

  def startListening(timeDifference: Double) = {

    val events = PublishChannel[Event]()
    
    val socket = socketio.io.connect()
    socket.on("cmd", callback = (input: js.Dynamic) => {
      val formatters = Serializables.getFormatter(input._type.toString)
      val formatted = formatters.fromJsDynamic(input)
      if (formatted.isSuccess) formatted.get match {
        case bd: BeatDelta => events.pushNext(bd)
        case fe: FilterEvent => events.pushNext(fe)
      }
    })

    val beats = events.collect {
      case bd: BeatDelta => bd
    }

    val filters = events.collect {
      case fe: FilterEvent => fe
    }

    val nested = Observable.create { o =>
      beats.foreach { msg =>
        val blinkCorrection = -25
        val originallyStarted = msg.timestamp + timeDifference + blinkCorrection
        val timeTillNextBeat = (Date.now() - originallyStarted) % msg.beatDelta

        Observable.unitDelayed(timeTillNextBeat.milliseconds, ()).foreach { _ =>
          o.observer.onNext(Observable.interval(msg.beatDelta.milliseconds))
        }
      }
    }: Observable[Observable[Long]]

    val flattened = switch(nested)

    val Timer = ReactComponentB[Unit]("Timer")
      .initialState(State("off", 64))
      .backend(new Backend(_, flattened, filters))
      .render($ => <.div(^.className := $.state.className, getStyle($.state.intensity)))
      .buildU

    React.render(Timer(), dom.document.body)
  }
}