package poormansdiscoscalajs.server

import scala.scalajs.js
import scala.scalajs.js.{Date, JSApp}
import monifu.reactive._
import monifu.concurrent.Scheduler
import monifu.concurrent.Implicits.globalScheduler
import scala.scalajs.js.annotation.JSExport


@JSExport
case class Event(val deltaTime: Double, val message: Int)

object PoorMansDisco extends JSApp {

  def messages(implicit scheduler: Scheduler): Observable[Event] =
    Observable.create { o =>
      js.Dynamic.global.eventreceived = (m:Event) => m match {
        // 248 is the code for a timecode signal
        case x: Event if x.message == 248 => o.onNext(x)
      }
    }

  def main(): Unit = {
    messages
      .map(_.deltaTime)
      .buffer(48)
      .map(x => x.sum / x.length)
      .map(x => 1/x*10)
      .foreach(x => js.Dynamic.global.sendMessage("something", x))
  }

  @JSExport
  def getServerTime: Double = {
    Date.now()
  }
}