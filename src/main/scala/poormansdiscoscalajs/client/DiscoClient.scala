package poormansdiscoscalajs.client

import scala.scalajs.js.{JSON, Date, JSApp}
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import dom.ext.Ajax
import scalajs.concurrent
        .JSExecutionContext
        .Implicits
        .runNow

object DiscoClient extends JSApp{
  @JSExport
  override def main(): Unit = {
    val requestDate = Date.now()
    for {
      response <- Ajax.get("/getServerTime")
      serverTime = JSON.parse(response.responseText).timestamp.toString().toDouble
      timeDifference = getTimeDifference(requestDate, Date.now(), serverTime)
    } yield println(s"Timedifference: $timeDifference")
  }

  def getTimeDifference(requestDate: Double, responseDate: Double, serverDate: Double) = {
    val estimatedLatency = (responseDate - requestDate) / 2
    serverDate - requestDate - estimatedLatency
  }

}
