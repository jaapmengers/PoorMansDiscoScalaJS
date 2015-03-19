package importedjs.socketio.client

import scala.scalajs.js

package object socketio extends js.GlobalScope {
  val io: SocketIO = js.native
}

trait SocketIO extends js.Object {
  def connect(): Socket = js.native
}

trait Socket extends js.Object {
  def on(event: String, callback: js.Any): Nothing = js.native
  def emit(event: String, data: js.Any): Nothing = js.native
}
