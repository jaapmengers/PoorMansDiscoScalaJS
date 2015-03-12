package poormansdiscoscalajs.client

import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import org.scalajs.dom
import poormansdiscoscalajs.shared.{BeatDelta, Serializables}

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

case class State(currentTime: Double)

class Backend($: BackendScope[_, State], ob: Observable[Double]) {
  ob.foreach(t => $.modState(s => State(t)))
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

  def startListening(timeDifference: Double) = {
    println(s"Timediference: $timeDifference")

    // Handle incoming beat signals
    def messages(implicit scheduler: Scheduler): Observable[BeatDelta] = {
      Observable.create { o =>
        js.Dynamic.global.eventreceived = (input: js.Dynamic) => {
          val formatter = Serializables.getFormatter(input._type.toString)
          formatter.fromJsDynamic(input) match {
            case beatDelta: BeatDelta => o.observer.onNext(beatDelta)
            case _ => throw new Exception("Type not yet supported")
          }

        }
      }
    }

    val nested = Observable.create { o =>
      messages.foreach { msg =>
        val originallyStarted = msg.timestamp + timeDifference
        val timeTillNextBeat = (Date.now() - originallyStarted) % msg.beatDelta
        println(s"Beatdelta ${msg.beatDelta}")
        Observable.unitDelayed(timeTillNextBeat.milliseconds, ()).foreach { _ =>
          o.observer.onNext(Observable.interval(msg.beatDelta.milliseconds))
        }
      }
    }: Observable[Observable[Long]]

    switch(nested).foreach{ x =>
      //do nothing
    }

//    val Timer = ReactComponentB[Unit]("Timer")
//      .initialState(State(0))
//      .backend(new Backend(_, messages))
//      .render($ => <.div("Seconds elapsed: ", $.state.currentTime))
//      .buildU
//
//    React.render(Timer(), dom.document.body)
  }
}
