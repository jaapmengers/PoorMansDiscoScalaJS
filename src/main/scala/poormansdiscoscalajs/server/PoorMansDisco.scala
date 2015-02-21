package poormansdiscoscalajs.server

import scala.scalajs.js
import scala.scalajs.js.JSApp
import monifu.reactive._
import monifu.concurrent.Scheduler
import monifu.concurrent.Implicits.globalScheduler
import scala.scalajs.js.annotation.JSExport


@JSExport
case class Event(val deltaTime: Double, val message: Int) {
  override def toString(): String = s"Event(${deltaTime},${message})"
}

object PoorMansDisco extends JSApp {

  def messages(implicit scheduler: Scheduler): Observable[Event] =
    Observable.create { o =>
      js.Dynamic.global.eventreceived = (m:Event) => o.onNext(m)
    }

  def main(): Unit = {
    messages.foreach(e => println(s"Message: ${e.message}, deltaTime: ${e.deltaTime}"))
  }
}