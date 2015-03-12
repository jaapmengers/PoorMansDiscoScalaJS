package poormansdiscoscalajs.shared

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

object Serializables {
  val allFormatters: Map[String, Formatter[_]] = List(BeatDeltaFormatter).map( t => t._type -> t ).toMap

  def getFormatter(_type: String) = {
    val formatOpt = allFormatters.get(_type)
    if(formatOpt.isDefined){
      formatOpt.get
    } else throw new Exception(s"No formatter found for type ${_type}")
  }
}

trait Formatter[T] {
  def toJsDynamic(a: T): js.Dynamic
  def fromJsDynamic(value: js.Dynamic): T
  val _type: String
}

@JSExport
case class Event(val deltaTime: Double, val message: Int)

@JSExport
case class BeatDelta(val beatDelta: Double, val timestamp: Long)

case object BeatDeltaFormatter extends Formatter[BeatDelta] {
  override def toJsDynamic(a: BeatDelta): js.Dynamic = js.Dynamic.literal("beatDelta" -> a.beatDelta, "timestamp" -> a.timestamp, "_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): BeatDelta = BeatDelta(value.beatDelta.toString.toDouble, value.timestamp.toString.toLong)
  override val _type: String = "BeatDelta"
}

case object UnitFormatter extends Formatter[Unit] {
  override def toJsDynamic(a: Unit): js.Dynamic = js.Dynamic.literal("_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): Unit = ()
  override val _type: String = "Unit"
}

case object StringFormatter extends Formatter[String] {
  override def toJsDynamic(a: String): js.Dynamic = js.Dynamic.literal("_type" -> _type, "value" -> a)
  override def fromJsDynamic(value: js.Dynamic): String = value.value.toString
  override val _type: String = "String"
}

case object ServerTimeResponseFormatter extends Formatter[GetServerTimeResponse] {
  override def toJsDynamic(a: GetServerTimeResponse): js.Dynamic = js.Dynamic.literal("timestamp" -> a.timestamp, "_type" -> _type)
  override def fromJsDynamic(value: js.Dynamic): GetServerTimeResponse = GetServerTimeResponse(value.timestamp.toString.toLong)
  override val _type: String = "GetServerTimeResponse"
}

case class GetServerTimeResponse(val timestamp: Long)

object Formatters {
  implicit val beatDeltaFormatter = BeatDeltaFormatter
  implicit val unitFormatter = UnitFormatter
  implicit val stringFormatter = StringFormatter
  implicit val serverTimeResponseFormatter = ServerTimeResponseFormatter
}
