package importedjs

import scala.scalajs.js

package object Midi extends js.GlobalScope {
  val midiInput: MidiInput = js.native
}

 trait MidiInput extends js.Object {
   def on(event: String, callback: js.Function2[Double, js.Array[Int], Unit]): Nothing = js.native
 }