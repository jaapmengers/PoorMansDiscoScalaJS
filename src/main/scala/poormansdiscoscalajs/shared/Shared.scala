package poormansdiscoscalajs.shared

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.util.Try

object Serializables {
  val allFormatters: Map[String, Formatter[_]] = List(BeatDeltaFormatter, FilterEventFormatter).map( t => t._type -> t ).toMap

  def getFormatter(_type: String) = {
    val formatOpt = allFormatters.get(_type)
    if(formatOpt.isDefined){
      formatOpt.get
    } else throw new Exception(s"No formatter found for type ${_type}")
  }
}

trait Formatter[T] {
  def toJsDynamic(a: T): js.Dynamic
  def fromJsDynamic(value: js.Dynamic): Try[T]
  val _type: String
}

trait Event
case class BeatEvent(val deltaTime: Double) extends Event
case class FilterEvent(val which: Int, val filterItensity: Int) extends Event
case class BeatDelta(val beatDelta: Double, val timestamp: Long) extends Event

@JSExport
case class MidiEvent(val deltaTime: Double, val message: js.Array[Int])



case object BeatDeltaFormatter extends Formatter[BeatDelta] {
  override def toJsDynamic(a: BeatDelta): js.Dynamic = js.Dynamic.literal("beatDelta" -> a.beatDelta, "timestamp" -> a.timestamp, "_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): Try[BeatDelta] =  Try(BeatDelta(value.beatDelta.toString.toDouble, value.timestamp.toString.toLong))
  override val _type: String = "BeatDelta"
}

case object UnitFormatter extends Formatter[Unit] {
  override def toJsDynamic(a: Unit): js.Dynamic = js.Dynamic.literal("_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): Try[Unit] = Try(())
  override val _type: String = "Unit"
}

case object StringFormatter extends Formatter[String] {
  override def toJsDynamic(a: String): js.Dynamic = js.Dynamic.literal("_type" -> _type, "value" -> a)
  override def fromJsDynamic(value: js.Dynamic): Try[String] = Try(value.value.toString)
  override val _type: String = "String"
}

case object ServerTimeResponseFormatter extends Formatter[GetServerTimeResponse] {
  override def toJsDynamic(a: GetServerTimeResponse): js.Dynamic = js.Dynamic.literal("timestamp" -> a.timestamp, "_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): Try[GetServerTimeResponse] = Try(GetServerTimeResponse(value.timestamp.toString.toLong))
  override val _type: String = "GetServerTimeResponse"
}

case object FilterEventFormatter extends Formatter[FilterEvent] {
  override def toJsDynamic(a: FilterEvent): js.Dynamic = js.Dynamic.literal("intensity" -> a.filterItensity, "which" -> a.which, "_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): Try[FilterEvent] = Try(FilterEvent(which = value.which.toString.toInt,  value.intensity.toString.toInt))
  override val _type: String = "FilterEvent"
}

case class GetServerTimeResponse(val timestamp: Long)

object Formatters {
  implicit val beatDeltaFormatter = BeatDeltaFormatter
  implicit val unitFormatter = UnitFormatter
  implicit val stringFormatter = StringFormatter
  implicit val serverTimeResponseFormatter = ServerTimeResponseFormatter
  implicit val filterEventFormatter = FilterEventFormatter
}
