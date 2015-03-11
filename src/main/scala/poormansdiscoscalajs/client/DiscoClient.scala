package poormansdiscoscalajs.client

import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import org.scalajs.dom

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
    } yield println(s"Timedifference: $timeDifference")

    React.render(Timer(), dom.document.body)
  }

  // Handle incoming beat signals
  def messages(implicit scheduler: Scheduler): Observable[Double] =
    Observable.create { o =>
      js.Dynamic.global.eventreceived = (m:Double) => o.onNext(m)
    }

  val obs = Observable.interval(1.second)

  val Timer = ReactComponentB[Unit]("Timer")
    .initialState(State(0))
    .backend(new Backend(_, messages))
    .render($ => <.div("Seconds elapsed: ", $.state.currentTime))
    .buildU

  def getTimeDifference(requestDate: Double, responseDate: Double, serverDate: Double) = {
    val estimatedLatency = (responseDate - requestDate) / 2
    serverDate - requestDate - estimatedLatency
  }
}
