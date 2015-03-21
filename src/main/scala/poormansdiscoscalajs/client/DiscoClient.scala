package poormansdiscoscalajs.client

import importedjs.socketio.client.socketio
import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.channels.PublishChannel
import org.scalajs.dom
import poormansdiscoscalajs.shared._

import concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.{JSON, Date, JSApp}
import scala.scalajs.js.annotation.JSExport
import japgolly.scalajs.react.{BackendScope, React, ReactComponentB}
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.concurrent.Implicits.globalScheduler

import dom.ext.Ajax
import scalajs.concurrent
        .JSExecutionContext
        .Implicits
import poormansdiscoscalajs.shared.BeatDelta
import scala.Some
import poormansdiscoscalajs.shared.FilterEvent

case class State(className: String, intensity: Int)

class Backend($: BackendScope[_, State], beatObs: Observable[Long], filterObs: Observable[FilterEvent]) {
  beatObs.foreach { t =>
    $.modState(s => State("on", s.intensity))
    Observable.unitDelayed(100.milliseconds, ()).foreach { _ =>
      $.modState(s => State("off", s.intensity))
    }
  }

  filterObs.foreach { t =>
    $.modState(s => State(s.className, t.filterItensity))
  }
}

object DiscoClient extends JSApp{
  @JSExport
  override def main(): Unit = {
    val requestDate = Date.now()
    for {
      response <- Ajax.get("/getServerTime")
      serverTime = JSON.parse(response.responseText).timestamp.toString().toDouble
      timeDifference = getTimeDifference(requestDate, Date.now(), serverTime)
    } yield startListening(timeDifference)

    /**
     * - Switch in een functie onderbrengen en kijken of ie als infix gebruikt kan worden
     * - Blocken op timedifference oid
     * - On eventReceived een nieuwe observable maken
     * - Uitrekenen hoeveel tijd er verstreken is sinds de server het bericht verstuurd heeft
     * - Afwachten tot de eerstvolgende beat is
     * - Observable maken getimed op frequentie van beats en die toevoegen aan observable van observables
     */
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
    println(s"Timediference: $timeDifference")

    val events = PublishChannel[Event]()
    
    val socket = socketio.io.connect()
    socket.on("cmd", (input: js.Dynamic) => {
      val formatters = Serializables.getFormatter(input._type.toString)
      formatters.fromJsDynamic(input) match {
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


    /**
     * Tijdsverschil: client loopt 5 seconden voor op server
     * Latency, eenrichting berekend op 20
     *
     * Server verstuurt bericht op 24, met interval van 7
     * Client ontvangt op 44
     * Eerstvolgende moment is: 44-2
     *
     * Client loop 2 miliseconde voor op server
     * Dwz als server zegt dat iets op 5 moet gebeuren, dan is dat op de client bij 7
     *
     * Server: 1-2-3-4-5-6-7-8-9
     * Client:       1-2-3-4-5-6-7-8-9
     *
     * GetServerTime op 1, response op 5, tijd op server is 6
     * Latency eenrichting = 5-1/2 = 2
     *
     * Request + 2 = 3. 3 bij client is 6 op server, server loopt 3 voor
     *
     * Dus als server zegt, dit moet nu (6) gebeuren, dan moet client daar 3 van aftrekken
     *
     * 0 - (6 - 1 - 2) = 3 0
     *
     *
     *
     *
     * val estimatedLatency = (responseDate - requestDate) / 2
     * serverDate - requestDate - estimatedLatency
     *
     * (40 - 0)/2 = 20
     */

    val flattened = switch(nested)

    val Timer = ReactComponentB[Unit]("Timer")
      .initialState(State("off", 64))
      .backend(new Backend(_, flattened, filters))
      .render($ => <.div(^.className := $.state.className, getStyle($.state.intensity)))
      .buildU

    React.render(Timer(), dom.document.body)
  }
}